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
    generate_allocation_explanation, normalize_driver
)
from config import (
    PRIORITY_WEIGHTS, PRIORITY_WATERFALL_MULTIPLIER, SOLVER_TYPE,
    EFFORT_ESTIMATE_WEIGHT, SKILL_MATCH_WEIGHT, PRIORITY_WEIGHT,
    REGION_DIVERSITY_WEIGHT
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
    # Track funding_source groups and driver caps
    funding_source_groups = defaultdict(list)  # funding_source -> list of project_ids
    funding_source_budgets = defaultdict(float)  # funding_source -> total budget
    driver_caps = {}  # (funding_source, driver) -> total cap (sum of alloc_budget for projects in that driver+funding_source)
    driver_funding_source_groups = defaultdict(lambda: defaultdict(list))  # (funding_source, driver) -> list of project_ids
    
    for _, proj_row in projects_df.iterrows():
        pid = int(proj_row['project_id'])
        priority = calculate_project_priority(proj_row, PRIORITY_WEIGHTS)
        project_priorities[pid] = priority
        
        funding_source = str(proj_row.get('funding_source', '')).strip()
        driver = str(proj_row.get('driver', '')).strip()
        alloc_budget = float(proj_row.get('alloc_budget', 0))
        
        # Track funding_source groups and budgets
        if funding_source:
            funding_source_groups[funding_source].append(pid)
            funding_source_budgets[funding_source] += alloc_budget
        
        # Track driver caps within funding_source
        if funding_source and driver:
            key = (funding_source, driver)
            if key not in driver_caps:
                driver_caps[key] = 0.0
            driver_caps[key] += alloc_budget
            driver_funding_source_groups[funding_source][driver].append(pid)
    
    # Calculate rank within (funding_source + driver) combination
    # User-entered rank field determines order within each (funding_source, driver) group
    driver_project_ranks = {}  # (funding_source, driver, project_id) -> rank within (funding_source + driver)
    
    for funding_source, drivers_dict in driver_funding_source_groups.items():
        for driver, project_ids in drivers_dict.items():
            # Get projects with their user-entered rank field
            project_ranks = []
            for pid in project_ids:
                proj_row = projects_df[projects_df['project_id'] == pid].iloc[0]
                user_rank = proj_row.get('rank', None)
                # Convert rank to number (lower rank number = higher priority)
                try:
                    rank_num = float(user_rank) if pd.notna(user_rank) else 999
                except:
                    rank_num = 999
                project_ranks.append((pid, rank_num, project_priorities[pid]))
            
            # Sort by user-entered rank (ascending: rank 1 = highest), then by priority as tiebreaker
            project_ranks.sort(key=lambda x: (x[1], -x[2]))  # Sort by rank (asc), then priority (desc)
            
            # Assign ranks within (funding_source + driver) (1 = highest priority)
            for rank, (pid, _, _) in enumerate(project_ranks, start=1):
                driver_project_ranks[(funding_source, driver, pid)] = rank
    
    # Calculate funding source priorities (max priority of projects in that source)
    funding_source_priorities = {}
    for funding_source, project_ids in funding_source_groups.items():
        priorities_in_source = [project_priorities[pid] for pid in project_ids]
        if priorities_in_source:
            funding_source_priorities[funding_source] = max(priorities_in_source)
        else:
            funding_source_priorities[funding_source] = 0.5
    
    # Sort projects by priority (highest first) for waterfall
    projects_df['priority_score'] = projects_df['project_id'].map(project_priorities)
    projects_df = projects_df.sort_values('priority_score', ascending=False).reset_index(drop=True)
    
    # Sort funding sources by priority (highest first) - NEW TOP LEVEL
    sorted_funding_sources = sorted(funding_source_priorities.items(), key=lambda x: x[1], reverse=True)
    
    # Calculate driver priorities within each funding source
    # Driver priority is based on driver type (normalize_driver)
    driver_priorities = {}
    for (funding_source, driver) in driver_caps.keys():
        driver_priorities[(funding_source, driver)] = normalize_driver(driver)
    
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
        
        driver = str(proj_row.get('driver', '')).strip()
        funding_source = str(proj_row.get('funding_source', '')).strip()
        
        # Get driver priority within funding source
        driver_priority = driver_priorities.get((funding_source, driver), 0.5) if funding_source and driver else 0.5
        
        # Get funding source priority
        funding_source_priority = funding_source_priorities.get(funding_source, 0.5)
        
        # Get rank within (funding_source + driver) combination
        driver_rank = driver_project_ranks.get((funding_source, driver, pid), 999)  # Higher rank = lower priority
        
        # Max resource allocation percentage (defaults to 1.0 = 100% if not specified)
        max_resource_allocation_pct = float(proj_row.get('max_resource_allocation_pct', 1.0))
        if pd.isna(max_resource_allocation_pct) or max_resource_allocation_pct <= 0:
            max_resource_allocation_pct = 1.0  # Default to 100% (no limit)
        max_resource_allocation_pct = min(max_resource_allocation_pct, 1.0)  # Cap at 100%
        
        # Get driver cap within funding source
        driver_cap = driver_caps.get((funding_source, driver), 0.0) if funding_source and driver else 0.0
        
        project_data[pid] = {
            'required_skills': req_skills,
            'alloc_budget': float(proj_row.get('alloc_budget', 0)),
            'effort_estimate': float(proj_row.get('effort_estimate_man_months', 0)) if pd.notna(proj_row.get('effort_estimate_man_months')) else None,
            'months': months,
            'priority': project_priorities[pid],
            'project_name': str(proj_row.get('project_name', f'Project {pid}')),
            'is_efficiency': float(proj_row.get('alloc_budget', 0)) == 0,
            'driver': driver,
            'driver_priority': driver_priority,
            'driver_cap': driver_cap,
            'funding_source': funding_source,
            'funding_source_priority': funding_source_priority,
            'driver_rank': driver_rank,  # Rank within (funding_source + driver)
            'max_resource_allocation_pct': max_resource_allocation_pct
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
    resource_locations = {}  # Cache resource locations for region diversity
    
    # Build resource location map
    for _, resource_row in resources_df.iterrows():
        resource_id = str(resource_row['brid'])
        location = str(resource_row.get('location', '')).strip()
        resource_locations[resource_id] = location
    
    # Get all unique locations
    all_locations = set(resource_locations.values())
    max_regions = len(all_locations)  # Maximum possible regions for diversity calculation
    
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
    
    # Constraint 4: Funding Source budget constraint - prevent budget leakage between funding sources
    # Sum of allocations per funding_source ≤ total budget for that funding_source
    for funding_source, total_budget in funding_source_budgets.items():
        if total_budget > 0:
            # Sum all allocations for projects in this funding_source across all months
            constraint = solver.Constraint(0.0, total_budget, f'funding_source_budget_{funding_source}')
            for (rid, pid, month), var in variables.items():
                proj_info = project_data.get(pid)
                if proj_info and proj_info.get('funding_source') == funding_source:
                    constraint.SetCoefficient(var, 1.0)
    
    # Constraint 3: Max resource allocation percentage per project (PER MONTH)
    # For projects with max_resource_allocation_pct < 1.0, limit how much of a resource
    # can be allocated to that project in EACH MONTH (not annual total).
    # This constraint is applied separately for each month, ensuring monthly limits.
    # Example: If max_pct=0.30 and resource costs £10k/month:
    #   - Month 1: allocation <= £3,000 (30% of monthly cost)
    #   - Month 2: allocation <= £3,000 (30% of monthly cost)
    #   - Month 3: allocation <= £3,000 (30% of monthly cost)
    #   Each month is independently constrained.
    for pid, proj_info in project_data.items():
        max_pct = proj_info.get('max_resource_allocation_pct', 1.0)
        if max_pct < 1.0:  # Only add constraint if less than 100%
            for resource_id in resource_ids:
                resource_row = resources_df[resources_df['brid'] == resource_id].iloc[0]
                resource_monthly_cost = float(resource_row['cost_per_month'])
                max_allocation_per_month = max_pct * resource_monthly_cost
                
                # Add a SEPARATE constraint for EACH month this project is active
                # Each month gets its own independent limit
                for month in proj_info['months']:
                    constraint = solver.Constraint(0.0, max_allocation_per_month, 
                                                  f'max_resource_pct_p{pid}_r{resource_id}_m{month}')
                    # Find variable for this resource-project-month combination
                    var_key = (resource_id, pid, month)
                    if var_key in variables:
                        constraint.SetCoefficient(variables[var_key], 1.0)
    
    # Objective function: Maximize allocation with preferences
    objective = solver.Objective()
    objective.SetMaximization()
    
    # Calculate max priority for waterfall multiplier
    max_priority = max(project_priorities.values()) if project_priorities else 1.0
    max_driver_priority = max([p for p in driver_priorities.values()]) if driver_priorities else 1.0
    max_funding_source_priority = max([p for p in funding_source_priorities.values()]) if funding_source_priorities else 1.0
    max_driver_rank = max([r for r in driver_project_ranks.values()]) if driver_project_ranks else 1
    
    # NEW HIERARCHY: Funding Source (strongest) > Driver (within funding source) > Rank (within funding_source + driver) > Priority
    # Funding source waterfall multiplier (strongest - highest priority)
    funding_source_waterfall_multiplier = waterfall_multiplier * 20.0  # Strongest
    # Driver waterfall multiplier (within funding source)
    driver_waterfall_multiplier = waterfall_multiplier * 10.0  # Strong, but less than funding source
    # Driver rank multiplier (rank within funding_source + driver)
    driver_rank_waterfall_multiplier = waterfall_multiplier * 5.0  # Medium strength
    
    # Pre-calculate region diversity for each project
    # Track which regions have variables for each project (for diversity bonus)
    # Projects with allocations from more diverse regions get higher bonus
    project_region_variable_counts = defaultdict(lambda: defaultdict(int))  # project_id -> {region: count}
    
    # First pass: count variables per region per project
    for (resource_id, pid, month), var in variables.items():
        resource_location = resource_locations.get(resource_id, '')
        if resource_location:
            project_region_variable_counts[pid][resource_location] += 1
    
    # Calculate region diversity bonus for each variable
    for (resource_id, pid, month), var in variables.items():
        resource_row = resources_df[resources_df['brid'] == resource_id].iloc[0]
        proj_info = project_data[pid]
        resource_location = resource_locations.get(resource_id, '')
        
        # Base coefficient: cost allocated (maximize allocation)
        # Use a very large base coefficient to prioritize full utilization
        # For budgeted projects, use extremely high base to ensure they get filled first
        # This ensures the solver maximizes allocation to budgeted projects before efficiency projects
        if proj_info['is_efficiency']:
            base_coeff = 1.0  # Lower base for efficiency projects
        else:
            base_coeff = 100000.0  # Extremely high base for budgeted projects to ensure full utilization
        
        # NEW HIERARCHY ORDER:
        # 1. Funding Source (waterfall - strongest, don't move budget between funding sources)
        funding_source_priority = proj_info.get('funding_source_priority', 0.5)
        funding_source_factor = 1.0 / (funding_source_waterfall_multiplier ** (max_funding_source_priority - funding_source_priority))
        funding_source_coeff = priority_weight * funding_source_factor * 20.0  # Strongest preference
        
        # 2. Driver priority multiplier (within funding source - driver waterfall effect)
        driver_priority = proj_info.get('driver_priority', 0.5)
        driver_factor = 1.0 / (driver_waterfall_multiplier ** (max_driver_priority - driver_priority))
        driver_coeff = priority_weight * driver_factor * 10.0  # Very strong preference
        
        # 3. Rank within (funding_source + driver) combination (user-entered rank)
        driver_rank = proj_info.get('driver_rank', 999)
        rank_factor = 1.0 / (driver_rank_waterfall_multiplier ** (driver_rank - 1))  # Rank 1 gets highest factor
        rank_coeff = priority_weight * rank_factor * 5.0  # Strong preference
        
        # Project priority multiplier (waterfall effect within funding_source and driver)
        priority = proj_info['priority']
        priority_factor = 1.0 / (waterfall_multiplier ** (max_priority - priority))
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
        
        # Region diversity bonus (soft constraint)
        # Strategy: Give higher bonus to allocations from regions that are less common for this project
        # This encourages the solver to spread allocations across regions
        region_diversity_bonus = 0.0
        if REGION_DIVERSITY_WEIGHT > 0 and resource_location and max_regions > 0:
            # Count how many variables this project has from each region
            region_counts = project_region_variable_counts[pid]
            total_vars_for_project = sum(region_counts.values())
            
            if total_vars_for_project > 0:
                # Calculate how common this region is for this project
                region_frequency = region_counts.get(resource_location, 0) / total_vars_for_project
                # Inverse frequency: less common regions get higher bonus
                # Bonus = REGION_DIVERSITY_WEIGHT * (1 - region_frequency) * normalization_factor
                # Normalize by max_regions to keep bonus small relative to base_coeff
                inverse_frequency = 1.0 - region_frequency
                region_diversity_bonus = REGION_DIVERSITY_WEIGHT * inverse_frequency * (1.0 / max_regions)
            else:
                # First allocation from this region - give small bonus
                region_diversity_bonus = REGION_DIVERSITY_WEIGHT * (1.0 / max_regions)
        
        # Total coefficient
        # Base coefficient (very high for budgeted projects) ensures full utilization is prioritized
        # NEW HIERARCHY: Funding Source > Driver > Rank (within funding_source+driver) > Priority
        # Region diversity bonus encourages cross-region allocation (higher for less common regions)
        total_coeff = base_coeff + funding_source_coeff + driver_coeff + rank_coeff + priority_coeff + skill_coeff + effort_coeff + region_diversity_bonus
        
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
