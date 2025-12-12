"""budget_allocator.py - Cost-based budget allocator using OR-Tools"""
import pandas as pd
import json
from datetime import datetime
from collections import defaultdict
from ortools.linear_solver import pywraplp
from typing import Dict, List, Tuple, Optional
from utils import (
    months_range, parse_required_skills, calculate_project_priority,
    check_mandatory_skills, calculate_skill_match_score, calculate_effort_alignment,
    generate_allocation_explanation
)
from config import (
    PRIORITY_WEIGHTS, PRIORITY_WATERFALL_MULTIPLIER, SOLVER_TYPE,
    EFFORT_ESTIMATE_WEIGHT, SKILL_MATCH_WEIGHT, PRIORITY_WEIGHT
)


def budget_allocator(resources_df: pd.DataFrame, projects_df: pd.DataFrame,
                    config: Optional[Dict] = None) -> List[Dict]:
    """Allocate resources to projects based on cost budgets.
    
    Args:
        resources_df: DataFrame with resource data (BRID, cost_per_year, skills, etc.)
        projects_df: DataFrame with project data (project_id, alloc_budget, dates, skills, etc.)
        config: Optional configuration dict (uses defaults from config.py if not provided)
    
    Returns:
        List of allocation dictionaries with keys:
        - resource_id (BRID)
        - resource_name
        - project_id
        - project_name
        - month (YYYY-MM)
        - allocated_cost
        - priority_score
        - skill_match_score
        - explanation
    """
    if config is None:
        config = {}
    
    # Use config defaults
    solver_type = config.get('solver_type', SOLVER_TYPE)
    waterfall_multiplier = config.get('priority_waterfall_multiplier', PRIORITY_WATERFALL_MULTIPLIER)
    effort_weight = config.get('effort_estimate_weight', EFFORT_ESTIMATE_WEIGHT)
    skill_weight = config.get('skill_match_weight', SKILL_MATCH_WEIGHT)
    priority_weight = config.get('priority_weight', PRIORITY_WEIGHT)
    
    # Prepare data
    resources_df = resources_df.copy()
    projects_df = projects_df.copy()
    
    # Calculate monthly costs for resources
    resources_df['cost_per_month'] = resources_df['cost_per_year'] / 12.0
    
    # Calculate project priorities
    project_priorities = {}
    for _, proj_row in projects_df.iterrows():
        pid = int(proj_row['project_id'])
        priority = calculate_project_priority(proj_row, PRIORITY_WEIGHTS)
        project_priorities[pid] = priority
    
    # Sort projects by priority (highest first) for waterfall
    projects_df['priority_score'] = projects_df['project_id'].map(project_priorities)
    projects_df = projects_df.sort_values('priority_score', ascending=False).reset_index(drop=True)
    
    # Parse project skills and dates
    project_data = {}
    project_months_map = {}
    all_months = set()
    
    for _, proj_row in projects_df.iterrows():
        pid = int(proj_row['project_id'])
        req_skills = parse_required_skills(proj_row.get('required_skills'))
        start_date = str(proj_row.get('start_date', '2025-01'))
        end_date = str(proj_row.get('end_date', '2025-12'))
        
        months = months_range(start_date, end_date)
        project_months_map[pid] = months
        all_months.update(months)
        
        project_data[pid] = {
            'required_skills': req_skills,
            'alloc_budget': float(proj_row.get('alloc_budget', 0)),
            'effort_estimate': float(proj_row.get('effort_estimate_man_months', 0)) if pd.notna(proj_row.get('effort_estimate_man_months')) else None,
            'months': months,
            'priority': project_priorities[pid],
            'project_name': str(proj_row.get('project_name', f'Project {pid}')),
            'is_efficiency': float(proj_row.get('alloc_budget', 0)) == 0
        }
    
    all_months = sorted(list(all_months))
    
    # Create solver
    if solver_type == 'CBC':
        solver = pywraplp.Solver.CreateSolver('CBC')
    else:
        solver = pywraplp.Solver.CreateSolver('GLOP')
    
    if not solver:
        raise RuntimeError('Could not create solver')
    
    # Decision variables: x[resource_id, project_id, month] = cost allocated
    variables = {}
    resource_ids = resources_df['brid'].tolist()
    skill_scores = {}  # Cache skill match scores
    
    # Create variables only for valid resource-project-month combinations
    for _, resource_row in resources_df.iterrows():
        resource_id = str(resource_row['brid'])
        resource_monthly_cost = float(resource_row['cost_per_month'])
        
        for pid, proj_info in project_data.items():
            # Check mandatory skills (hard constraint)
            can_allocate, _ = check_mandatory_skills(resource_row, proj_info['required_skills'])
            if not can_allocate:
                continue  # Skip - hard constraint
            
            # Calculate skill match score
            skill_match = calculate_skill_match_score(resource_row, proj_info['required_skills'])
            skill_scores[(resource_id, pid)] = skill_match
            
            # Create variables for each month in project timeline
            for month in proj_info['months']:
                var_name = f'x_{resource_id}_p{pid}_m{month}'
                # Variable represents cost allocated (not FTE)
                # Upper bound: resource monthly cost (can't allocate more than resource costs)
                var = solver.NumVar(0.0, resource_monthly_cost, var_name)
                variables[(resource_id, pid, month)] = var
    
    # Constraints
    
    # Constraint 1: Resource capacity - sum of allocations per resource-month ≤ monthly cost
    for resource_id in resource_ids:
        resource_row = resources_df[resources_df['brid'] == resource_id].iloc[0]
        resource_monthly_cost = float(resource_row['cost_per_month'])
        
        for month in all_months:
            constraint = solver.Constraint(0.0, resource_monthly_cost, f'resource_capacity_{resource_id}_{month}')
            for (rid, pid, m), var in variables.items():
                if rid == resource_id and m == month:
                    constraint.SetCoefficient(var, 1.0)
    
    # Constraint 2: Budget constraint - sum of allocations per project-month ≤ monthly budget
    # Only for projects with budget > 0
    for pid, proj_info in project_data.items():
        if proj_info['alloc_budget'] > 0:  # Only budgeted projects
            num_months = len(proj_info['months'])
            monthly_budget = proj_info['alloc_budget'] / num_months if num_months > 0 else 0
            
            for month in proj_info['months']:
                constraint = solver.Constraint(0.0, monthly_budget, f'budget_p{pid}_m{month}')
                for (rid, p, m), var in variables.items():
                    if p == pid and m == month:
                        constraint.SetCoefficient(var, 1.0)
    
    # Note: Efficiency projects (alloc_budget = 0) have no budget constraint
    # They can use unallocated resources up to resource capacity
    
    # Objective function: Maximize allocation with preferences
    objective = solver.Objective()
    objective.SetMaximization()
    
    # Calculate max priority for waterfall multiplier
    max_priority = max(project_priorities.values()) if project_priorities else 1.0
    
    for (resource_id, pid, month), var in variables.items():
        resource_row = resources_df[resources_df['brid'] == resource_id].iloc[0]
        proj_info = project_data[pid]
        
        # Base coefficient: cost allocated (maximize allocation)
        base_coeff = 1.0
        
        # Priority multiplier (waterfall effect)
        priority = proj_info['priority']
        priority_factor = base_coeff / (waterfall_multiplier ** (max_priority - priority))
        priority_coeff = priority_weight * priority_factor
        
        # Skill match preference
        skill_match = skill_scores.get((resource_id, pid), {'overall_score': 0.5})
        skill_coeff = skill_weight * skill_match['overall_score']
        
        # Effort estimate alignment (if available)
        effort_coeff = 0.0
        if proj_info['effort_estimate']:
            # Calculate target allocation for effort
            target_per_month = (proj_info['effort_estimate'] * resource_row['cost_per_month']) / len(proj_info['months'])
            # Prefer allocations closer to target (soft constraint)
            # This is approximated in objective - actual alignment calculated post-solution
            effort_coeff = effort_weight * 0.5  # Neutral preference
        
        # Total coefficient
        total_coeff = priority_coeff + skill_coeff + effort_coeff
        
        objective.SetCoefficient(var, total_coeff)
    
    # Solve
    status = solver.Solve()
    
    if status != pywraplp.Solver.OPTIMAL and status != pywraplp.Solver.FEASIBLE:
        raise RuntimeError(f'Solver failed with status: {status}')
    
    # Extract results
    allocations = []
    
    for (resource_id, pid, month), var in variables.items():
        allocated_cost = var.solution_value()
        
        if allocated_cost > 0.001:  # Only non-zero allocations
            resource_row = resources_df[resources_df['brid'] == resource_id].iloc[0]
            proj_info = project_data[pid]
            
            # Get skill scores
            skill_match = skill_scores.get((resource_id, pid), {'overall_score': 0.5})
            
            # Calculate effort alignment
            effort_alignment = calculate_effort_alignment(
                allocated_cost,
                float(resource_row['cost_per_month']),
                proj_info['effort_estimate'],
                len(proj_info['months'])
            )
            
            # Generate explanation using utility function
            priority_score = proj_info['priority']
            project_rank = None  # Could be calculated from sorted projects
            explanation = generate_allocation_explanation(
                resource_id=resource_id,
                resource_name=str(resource_row.get('employee_name', resource_id)),
                project_id=pid,
                project_name=proj_info['project_name'],
                allocated_cost=allocated_cost,
                priority_score=priority_score,
                skill_scores={
                    'overall_score': skill_match['overall_score'],
                    'technical_score': skill_match.get('technical_score', 0.0),
                    'functional_score': skill_match.get('functional_score', 0.0),
                    'mandatory_met': True  # Already filtered by mandatory skills check
                },
                effort_alignment=effort_alignment,
                is_efficiency_project=proj_info['is_efficiency'],
                project_rank=project_rank
            )
            
            allocations.append({
                'resource_id': resource_id,
                'resource_name': str(resource_row.get('employee_name', resource_id)),
                'project_id': pid,
                'project_name': proj_info['project_name'],
                'month': month,
                'allocated_cost': allocated_cost,
                'priority_score': priority_score,
                'skill_match_score': skill_match['overall_score'],
                'effort_alignment': effort_alignment,
                'explanation': explanation
            })
    
    return allocations
