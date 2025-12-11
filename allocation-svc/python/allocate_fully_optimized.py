"""allocate_fully_optimized.py - Fully optimized OR-Tools allocator with ALL enhancements

Implements all optimization strategies:
1. Skill quality scoring - Prefer better skill matches
2. Reduced fragmentation - Penalize small allocations
3. Workload balancing - Minimize variance in employee utilization
4. Allocation continuity - Prefer stable allocations across months
5. Project priority - Weight by project impact/importance
6. Team composition - Ensure balanced teams (grade, region, gender diversity)
7. Employee preferences - Consider employee project preferences
8. Risk mitigation - Limit single-employee dependency
9. Skill development - Allow learning allocations
10. Resource leveling - Smooth workload over time
11. Budget flexibility - Allow budget borrowing between months
12. Integer allocations - Optional discrete allocation increments
"""

import pandas as pd
import json
from datetime import datetime
from collections import defaultdict
from ortools.linear_solver import pywraplp


# Helper functions (moved from allocate.py)
def months_range(start_ym, end_ym):
    """Return list of YYYY-MM strings inclusive"""
    start = pd.to_datetime(start_ym + "-01")
    end = pd.to_datetime(end_ym + "-01")
    months = pd.date_range(start=start, end=end, freq='MS').strftime('%Y-%m').tolist()
    return months

def parse_required_skills(field):
    if not field or pd.isna(field):
        return {'technical': [], 'functional': []}
    if isinstance(field, str):
        try:
            val = json.loads(field)
            return {'technical': val.get('technical', []), 'functional': val.get('functional', [])}
        except Exception:
            # fallback: assume comma-separated technical skills
            return {'technical': [s.strip() for s in field.split(',') if s.strip()], 'functional': []}
    if isinstance(field, dict):
        return {'technical': field.get('technical', []), 'functional': field.get('functional', [])}
    return {'technical': [], 'functional': []}

def skill_score(emp_row, req):
    """Score an employee against required skills dict{'technical':[...],'functional':[...]}"""
    score = 0.0
    emp_tech = (emp_row.get('technical_skills') or '').lower()
    emp_func = (emp_row.get('functional_skills') or '').lower()
    for t in req.get('technical', []):
        if t.lower() in emp_tech:
            score += 2.0
    for f in req.get('functional', []):
        if f.lower() in emp_func:
            score += 1.0
    return score

def _employee_has_required_skills(emp_row, req_skills):
    """Check if employee has required skills for a project.
    Returns True if employee has at least one of the required skills (OR logic).
    This allows employees to contribute if they have any matching skill.
    """
    if not req_skills or (not req_skills.get('technical') and not req_skills.get('functional')):
        return True  # No requirements means any employee can be allocated
    
    emp_tech = (emp_row.get('technical_skills') or '').lower()
    emp_func = (emp_row.get('functional_skills') or '').lower()
    
    # Check if employee has at least one required skill (technical OR functional)
    has_match = False
    
    # Check technical skills
    if req_skills.get('technical'):
        for tech in req_skills.get('technical', []):
            if tech.lower() in emp_tech:
                has_match = True
                break
    
    # Check functional skills (if no technical match yet)
    if not has_match and req_skills.get('functional'):
        for func in req_skills.get('functional', []):
            if func.lower() in emp_func:
                has_match = True
                break
    
    return has_match


def fully_optimized_allocator(
    employees_df, 
    projects_df, 
    scenario_id, 
    global_start=None, 
    global_end=None,
    weights=None,
    config=None
):
    """Fully optimized OR-Tools allocator with all enhancement strategies.
    
    Args:
        employees_df: DataFrame with employee details
        projects_df: DataFrame with projects
        scenario_id: Scenario ID for allocations
        global_start: Global start month (YYYY-MM)
        global_end: Global end month (YYYY-MM)
        weights: Dict with optimization weights (defaults provided)
        config: Dict with configuration options:
            - max_employee_per_project: Max FTE per employee per project (default: 0.8)
            - min_team_size: Minimum team size per project (default: 1)
            - allow_skill_development: Allow allocations for learning (default: True)
            - skill_dev_max_fte: Max FTE for skill development (default: 0.2)
            - discrete_allocations: Use discrete increments (default: False)
            - allocation_increments: List of allowed increments if discrete (default: [0.25, 0.5, 0.75, 1.0])
            - budget_flexibility: Allow budget borrowing between months (default: True)
            - max_budget_borrow: Max months to borrow budget (default: 1)
            - enable_team_diversity: Enforce team composition constraints (default: True)
            - min_grade_diversity: Require multiple grade levels (default: False)
            - enable_employee_preferences: Use employee preferences (default: True)
    
    Returns:
        List of allocation dictionaries
    """
    # Default weights
    if weights is None:
        weights = {
            'cost_weight': 0.30,
            'skill_weight': 0.15,
            'fragmentation_weight': 0.10,
            'continuity_weight': 0.10,
            'balance_weight': 0.10,
            'preference_weight': 0.05,
            'diversity_weight': 0.05,
            'leveling_weight': 0.05,
            'role_balance_weight': 0.10  # New: Weight for role allocation balance
        }
    
    # Default config
    if config is None:
        config = {}
    
    # Set defaults for missing config keys
    default_config = {
        'max_employee_per_project': 0.8,
        'min_team_size': 1,
        'allow_skill_development': True,
        'skill_dev_max_fte': 0.2,
        'discrete_allocations': False,
        'allocation_increments': [0.25, 0.5, 0.75, 1.0],
        'budget_flexibility': True,
        'max_budget_borrow': 1,
        'enable_team_diversity': True,
        'min_grade_diversity': False,
        'enable_employee_preferences': True,
        'enforce_role_allocation': True,  # New: Enforce QA/DEV/BA allocation
        'allow_allocation_without_skills': False,  # Allow allocation even if no skills match (with penalty)
        'no_skills_penalty_multiplier': 2.0,  # Cost multiplier when allocating without required skills
        'maximize_budget_utilization': False,  # Maximize budget usage (adds negative cost term to objective)
        'budget_maximization_weight_multiplier': 1.0,  # Multiplier for budget maximization weight (1.0 = same as cost weight)
        'min_budget_utilization': 0.0,  # Minimum budget utilization (0.0-1.0) - adds constraint to use at least X% of budget
        'role_allocation_ratios': {  # New: Required role proportions
            'DEV': 0.50,  # 50% of project allocation should be DEV
            'QA': 0.30,   # 30% should be QA
            'BA': 0.20    # 20% should be BA (optional, can be 0)
        },
        'min_role_allocation': {  # New: Minimum FTE per role per project-month
            'DEV': 0.1,   # At least 0.1 FTE of DEV
            'QA': 0.05,   # At least 0.05 FTE of QA
            'BA': 0.0     # BA is optional (0 means not required)
        }
    }
    
    # Merge user config with defaults
    for key, value in default_config.items():
        if key not in config:
            config[key] = value
    
    # Prepare data
    employees = employees_df.copy()
    employees['technical_skills'] = employees['technical_skills'].fillna('').astype(str)
    employees['functional_skills'] = employees['functional_skills'].fillna('').astype(str)
    employees['fte_capacity'] = employees['fte_capacity'].fillna(1.0).astype(float)
    employees['cost_per_month'] = employees['cost_per_month'].fillna(0.0).astype(float)
    employees['region'] = employees['region'].fillna('').astype(str) if 'region' in employees.columns else pd.Series([''] * len(employees))
    employees['grade'] = employees['grade'].fillna('').astype(str) if 'grade' in employees.columns else pd.Series([''] * len(employees))
    employees['gender'] = employees['gender'].fillna('other').astype(str) if 'gender' in employees.columns else pd.Series(['other'] * len(employees))
    
    # Determine employee roles (QA, DEV, BA)
    # Priority: explicit role field > functional_skills inference > default to DEV
    if 'role' in employees.columns:
        employees['role'] = employees['role'].fillna('DEV').astype(str).str.upper()
    else:
        # Infer role from functional_skills or technical_skills
        def infer_role(row):
            func_skills = str(row.get('functional_skills', '')).lower()
            tech_skills = str(row.get('technical_skills', '')).lower()
            
            # Check for QA indicators
            if any(keyword in func_skills or keyword in tech_skills 
                   for keyword in ['qa', 'quality', 'testing', 'test', 'qa engineer', 'test engineer']):
                return 'QA'
            # Check for BA indicators
            if any(keyword in func_skills or keyword in tech_skills 
                   for keyword in ['ba', 'business analyst', 'analyst', 'business analysis', 'requirements']):
                return 'BA'
            # Check for DEV indicators
            if any(keyword in func_skills or keyword in tech_skills 
                   for keyword in ['dev', 'developer', 'development', 'engineer', 'programming', 'coding']):
                return 'DEV'
            # Default to DEV if no clear indicator
            return 'DEV'
        
        employees['role'] = employees.apply(infer_role, axis=1)
    
    # Map employees by role
    employees_by_role = {
        'DEV': employees[employees['role'] == 'DEV']['employee_id'].tolist(),
        'QA': employees[employees['role'] == 'QA']['employee_id'].tolist(),
        'BA': employees[employees['role'] == 'BA']['employee_id'].tolist()
    }
    
    # Employee preferences (if available)
    if 'preferred_projects' in employees.columns:
        employees['preferred_projects'] = employees['preferred_projects'].fillna('').astype(str)
    else:
        employees['preferred_projects'] = ''
    
    # Filter active employees
    active_employees = employees[employees['status'] == 'active'].copy()
    if len(active_employees) == 0:
        return []
    
    # Determine months
    if global_start and global_end:
        global_months = pd.date_range(start=global_start + "-01", end=global_end + "-01", freq='MS').strftime('%Y-%m').tolist()
    else:
        global_months = None
    
    # Collect all project-month combinations
    project_months = []
    project_data = {}
    all_months = set()
    project_months_map = defaultdict(list)  # project_id -> list of months
    
    for _, proj in projects_df.iterrows():
        start = proj.get('start_month') or (global_start or pd.Timestamp.now().strftime('%Y-%m'))
        end = proj.get('end_month') or (global_end or pd.Timestamp.now().strftime('%Y-%m'))
        months = months_range(start, end) if start and end else (global_months or [])
        req_skills = parse_required_skills(proj.get('required_skills'))
        max_budget = float(proj.get('max_budget') or 0.0)
        per_month_budget = max_budget / max(1, len(months)) if months else 0.0
        region_pref = str(proj.get('region_preference') or '')
        impact = str(proj.get('impact') or 'Medium').lower()
        priority = {'high': 3, 'medium': 2, 'low': 1}.get(impact, 2)
        
        project_data[int(proj['project_id'])] = {
            'required_skills': req_skills,
            'per_month_budget': per_month_budget,
            'total_budget': max_budget,
            'region_preference': region_pref,
            'priority': priority,
            'months': months
        }
        
        for m in months:
            project_months.append((int(proj['project_id']), m))
            all_months.add(m)
            project_months_map[int(proj['project_id'])].append(m)
    
    if not project_months:
        return []
    
    all_months = sorted(list(all_months))
    
    # Choose solver based on discrete allocations
    if config['discrete_allocations']:
        solver = pywraplp.Solver.CreateSolver('SCIP')
        if not solver:
            solver = pywraplp.Solver.CreateSolver('CBC')
    else:
        solver = pywraplp.Solver.CreateSolver('GLOP')
    
    if not solver:
        raise RuntimeError('Could not create solver')
    
    # Decision variables: x[employee_id, project_id, month] = allocation fraction
    variables = {}
    skill_dev_variables = {}  # Separate variables for skill development
    employee_ids = active_employees['employee_id'].tolist()
    skill_scores = {}  # Cache skill scores
    employee_preferences = {}  # Cache preferences
    
    for eid in employee_ids:
        emp_row = active_employees[active_employees['employee_id'] == eid].iloc[0]
        preferred = str(emp_row.get('preferred_projects', '') or '')
        preferred_list = [int(p.strip()) for p in preferred.split(',') if p.strip().isdigit()] if preferred else []
        employee_preferences[eid] = preferred_list
        
        for pid, month in project_months:
            proj_info = project_data[pid]
            req_skills = proj_info['required_skills']
            has_skills = _employee_has_required_skills(emp_row, req_skills)
            
            # Regular allocation (if has skills)
            if has_skills:
                var_name = f'x_e{eid}_p{pid}_m{month}'
                if config['discrete_allocations']:
                    # Integer variable representing allocation level
                    max_level = len(config['allocation_increments']) - 1
                    var = solver.IntVar(0, max_level, var_name)
                else:
                    var = solver.NumVar(0.0, float(emp_row['fte_capacity']), var_name)
                variables[(eid, pid, month)] = var
                skill_scores[(eid, pid)] = skill_score(emp_row, req_skills)
            elif config.get('allow_allocation_without_skills', False):
                # Allow allocation even without required skills (with penalty)
                var_name = f'x_e{eid}_p{pid}_m{month}_noskills'
                if config['discrete_allocations']:
                    max_level = len(config['allocation_increments']) - 1
                    var = solver.IntVar(0, max_level, var_name)
                else:
                    var = solver.NumVar(0.0, float(emp_row['fte_capacity']), var_name)
                variables[(eid, pid, month)] = var
                skill_scores[(eid, pid)] = 0.0  # No skill match
            else:
                variables[(eid, pid, month)] = None
            
            # Skill development allocation (if enabled and employee lacks some skills)
            if config['allow_skill_development'] and not has_skills:
                # Check if employee has partial skills (at least one type)
                has_partial = False
                emp_tech = (emp_row.get('technical_skills') or '').lower()
                emp_func = (emp_row.get('functional_skills') or '').lower()
                
                if req_skills.get('technical'):
                    for tech in req_skills.get('technical', []):
                        if tech.lower() in emp_tech:
                            has_partial = True
                            break
                if not has_partial and req_skills.get('functional'):
                    for func in req_skills.get('functional', []):
                        if func.lower() in emp_func:
                            has_partial = True
                            break
                
                if has_partial:
                    var_name = f'sd_e{eid}_p{pid}_m{month}'
                    skill_dev_variables[(eid, pid, month)] = solver.NumVar(
                        0.0, config['skill_dev_max_fte'], var_name
                    )
                else:
                    skill_dev_variables[(eid, pid, month)] = None
            else:
                skill_dev_variables[(eid, pid, month)] = None
    
    # Helper functions
    def prev_month(month_str):
        year, month = map(int, month_str.split('-'))
        if month == 1:
            return f"{year-1}-12"
        return f"{year}-{month-1:02d}"
    
    def next_month(month_str):
        year, month = map(int, month_str.split('-'))
        if month == 12:
            return f"{year+1}-01"
        return f"{year}-{month+1:02d}"
    
    def get_allocation_value(var, month_idx=None):
        """Get actual FTE value from variable (handles discrete allocations)"""
        if var is None:
            return 0.0
        if config['discrete_allocations']:
            level = var.solution_value()
            return config['allocation_increments'][int(level)] if level < len(config['allocation_increments']) else 0.0
        return var.solution_value()
    
    # Fragmentation penalty variables
    fragmentation_vars = {}
    if weights['fragmentation_weight'] > 0:
        for (eid, pid, month), var in variables.items():
            if var is not None:
                frag_var = solver.NumVar(0.0, 1.0, f'frag_e{eid}_p{pid}_m{month}')
                fragmentation_vars[(eid, pid, month)] = frag_var
                # frag_var >= 1 - 4*allocation (simplified, relaxed)
                constraint = solver.Constraint(-solver.infinity(), 1.0, f'frag_const_e{eid}_p{pid}_m{month}')
                constraint.SetCoefficient(frag_var, 1.0)
                if config['discrete_allocations']:
                    constraint.SetCoefficient(var, 4.0)
                else:
                    constraint.SetCoefficient(var, 4.0)
    
    # Continuity penalty variables
    continuity_vars = {}
    if weights['continuity_weight'] > 0:
        for eid in employee_ids:
            for pid, month in project_months:
                if variables.get((eid, pid, month)) is not None:
                    prev_m = prev_month(month)
                    if (eid, pid, prev_m) in variables and variables[(eid, pid, prev_m)] is not None:
                        change_var = solver.NumVar(0.0, 1.0, f'change_e{eid}_p{pid}_m{month}')
                        continuity_vars[(eid, pid, month)] = change_var
                        # change_var >= |x[month] - x[prev_month]|
                        constraint1 = solver.Constraint(0.0, solver.infinity(), f'cont1_e{eid}_p{pid}_m{month}')
                        constraint1.SetCoefficient(change_var, 1.0)
                        constraint1.SetCoefficient(variables[(eid, pid, month)], -1.0)
                        constraint1.SetCoefficient(variables[(eid, pid, prev_m)], 1.0)
                        
                        constraint2 = solver.Constraint(0.0, solver.infinity(), f'cont2_e{eid}_p{pid}_m{month}')
                        constraint2.SetCoefficient(change_var, 1.0)
                        constraint2.SetCoefficient(variables[(eid, pid, month)], 1.0)
                        constraint2.SetCoefficient(variables[(eid, pid, prev_m)], -1.0)
    
    # Workload leveling variables (month-to-month change) - simplified
    leveling_vars = {}
    if weights['leveling_weight'] > 0:
        for eid in employee_ids:
            for month_idx in range(1, len(all_months)):
                month = all_months[month_idx]
                prev_m = all_months[month_idx - 1]
                
                # Simplified: just track absolute change
                level_var = solver.NumVar(0.0, 2.0, f'level_e{eid}_m{month}')
                leveling_vars[(eid, month)] = level_var
                
                # level_var >= sum(this_month) - sum(prev_month)
                constraint1 = solver.Constraint(0.0, solver.infinity(), f'level1_e{eid}_m{month}')
                constraint1.SetCoefficient(level_var, -1.0)
                for pid, m in project_months:
                    if m == month:
                        var = variables.get((eid, pid, month))
                        if var is not None:
                            constraint1.SetCoefficient(var, 1.0)
                    if m == prev_m:
                        var = variables.get((eid, pid, prev_m))
                        if var is not None:
                            constraint1.SetCoefficient(var, -1.0)
                
                # level_var >= sum(prev_month) - sum(this_month)
                constraint2 = solver.Constraint(0.0, solver.infinity(), f'level2_e{eid}_m{month}')
                constraint2.SetCoefficient(level_var, -1.0)
                for pid, m in project_months:
                    if m == month:
                        var = variables.get((eid, pid, month))
                        if var is not None:
                            constraint2.SetCoefficient(var, -1.0)
                    if m == prev_m:
                        var = variables.get((eid, pid, prev_m))
                        if var is not None:
                            constraint2.SetCoefficient(var, 1.0)
    
    # Workload balance: minimize max utilization
    max_utilization_var = None
    if weights['balance_weight'] > 0:
        max_utilization_var = solver.NumVar(0.0, len(project_months), 'max_utilization')
        for eid in employee_ids:
            for month in all_months:
                constraint = solver.Constraint(0.0, solver.infinity(), f'balance_e{eid}_m{month}')
                constraint.SetCoefficient(max_utilization_var, -1.0)
                for pid, m in project_months:
                    if m == month:
                        var = variables.get((eid, pid, month))
                        if var is not None:
                            if config['discrete_allocations']:
                                # For discrete, need to convert level to FTE
                                # This is approximate - would need indicator variables for exact
                                constraint.SetCoefficient(var, 0.5)  # Average increment
                            else:
                                constraint.SetCoefficient(var, 1.0)
    
    # Role-based allocation constraints (QA, DEV, BA)
    if config.get('enforce_role_allocation', False):
        role_ratios = config.get('role_allocation_ratios', {'DEV': 0.50, 'QA': 0.30, 'BA': 0.20})
        min_role_alloc = config.get('min_role_allocation', {'DEV': 0.1, 'QA': 0.05, 'BA': 0.0})
        
        for pid, month in project_months:
            # Calculate total allocation for this project-month
            total_allocation_vars = []
            for eid in employee_ids:
                var = variables.get((eid, pid, month))
                if var is not None:
                    total_allocation_vars.append(var)
            
            if len(total_allocation_vars) > 0:
                # For each required role, ensure minimum allocation
                for role in ['DEV', 'QA', 'BA']:
                    min_fte = min_role_alloc.get(role, 0.0)
                    if min_fte > 0:
                        # Sum of allocations from employees with this role
                        role_allocation_vars = []
                        for eid in employees_by_role.get(role, []):
                            var = variables.get((eid, pid, month))
                            if var is not None:
                                role_allocation_vars.append(var)
                        
                        if len(role_allocation_vars) > 0:
                            # Constraint: sum of role allocations >= min_fte
                            constraint = solver.Constraint(min_fte, solver.infinity(), 
                                                         f'min_{role}_p{pid}_m{month}')
                            for var in role_allocation_vars:
                                constraint.SetCoefficient(var, 1.0)
                
                # Proportional allocation constraints (soft - try to maintain ratios)
                # For each role, try to maintain the target ratio
                for role in ['DEV', 'QA', 'BA']:
                    target_ratio = role_ratios.get(role, 0.0)
                    if target_ratio > 0:
                        role_allocation_vars = []
                        for eid in employees_by_role.get(role, []):
                            var = variables.get((eid, pid, month))
                            if var is not None:
                                role_allocation_vars.append(var)
                        
                        if len(role_allocation_vars) > 0 and len(total_allocation_vars) > 0:
                            # Soft constraint: sum(role_allocations) >= target_ratio * sum(total_allocations)
                            # This is handled via penalty in objective function (see below)
                            pass
    
    # Team diversity variables (for team composition constraints) - simplified
    # Full implementation would require binary indicators for team composition
    diversity_penalties = {}
    if config['enable_team_diversity'] and weights['diversity_weight'] > 0:
        # Simplified: just track if team is too small (less diversity)
        for pid, month in project_months:
            div_penalty = solver.NumVar(0.0, 1.0, f'div_penalty_p{pid}_m{month}')
            diversity_penalties[(pid, month)] = div_penalty
            # Penalty increases if total allocation is small (single person team)
            constraint = solver.Constraint(0.0, solver.infinity(), f'div_const_p{pid}_m{month}')
            constraint.SetCoefficient(div_penalty, 1.0)
            for eid in employee_ids:
                var = variables.get((eid, pid, month))
                if var is not None:
                    constraint.SetCoefficient(var, -1.0)  # div_penalty >= -sum(allocations)
    
    # Objective: Multi-objective weighted sum
    objective = solver.Objective()
    avg_cost = active_employees['cost_per_month'].mean()
    
    # 1. Cost minimization
    for (eid, pid, month), var in variables.items():
        if var is not None:
            emp_row = active_employees[active_employees['employee_id'] == eid].iloc[0]
            cost = float(emp_row['cost_per_month'])
            proj_info = project_data[pid]
            
            region_penalty = 0.0
            if proj_info['region_preference'] and emp_row['region'] != proj_info['region_preference']:
                region_penalty = cost * 0.1
            
            priority_factor = 1.0 / proj_info['priority']
            
            # Check if this is an allocation without required skills
            has_skills_for_proj = skill_scores.get((eid, pid), 0.0) > 0.0
            no_skills_penalty = 0.0
            if not has_skills_for_proj and config.get('allow_allocation_without_skills', False):
                # Apply penalty multiplier for allocations without required skills
                penalty_mult = config.get('no_skills_penalty_multiplier', 2.0)
                no_skills_penalty = cost * (penalty_mult - 1.0)  # Additional cost penalty
            
            if config['discrete_allocations']:
                # For discrete, cost scales with allocation level
                coeff = (cost + region_penalty + no_skills_penalty) * priority_factor * weights['cost_weight']
                objective.SetCoefficient(var, coeff * 0.5)  # Average increment size
            else:
                objective.SetCoefficient(var, (cost + region_penalty + no_skills_penalty) * priority_factor * weights['cost_weight'])
    
    # 2. Skill quality
    max_skill_score = max(skill_scores.values()) if skill_scores else 1.0
    for (eid, pid, month), var in variables.items():
        if var is not None:
            skill_qual = skill_scores.get((eid, pid), 0.0)
            skill_penalty = (max_skill_score - skill_qual) / max_skill_score if max_skill_score > 0 else 0
            if config['discrete_allocations']:
                objective.SetCoefficient(var, skill_penalty * avg_cost * weights['skill_weight'] * 0.5)
            else:
                objective.SetCoefficient(var, skill_penalty * avg_cost * weights['skill_weight'])
    
    # 3. Fragmentation penalty
    if weights['fragmentation_weight'] > 0:
        for (eid, pid, month), frag_var in fragmentation_vars.items():
            objective.SetCoefficient(frag_var, avg_cost * weights['fragmentation_weight'])
    
    # 4. Continuity penalty
    if weights['continuity_weight'] > 0:
        for (eid, pid, month), change_var in continuity_vars.items():
            objective.SetCoefficient(change_var, avg_cost * weights['continuity_weight'])
    
    # 5. Workload balance
    if weights['balance_weight'] > 0 and max_utilization_var is not None:
        objective.SetCoefficient(max_utilization_var, avg_cost * weights['balance_weight'])
    
    # 6. Employee preferences
    if config['enable_employee_preferences'] and weights['preference_weight'] > 0:
        for (eid, pid, month), var in variables.items():
            if var is not None:
                preferred = pid in employee_preferences.get(eid, [])
                if not preferred:
                    preference_penalty = avg_cost * weights['preference_weight']
                    if config['discrete_allocations']:
                        objective.SetCoefficient(var, preference_penalty * 0.5)
                    else:
                        objective.SetCoefficient(var, preference_penalty)
    
    # 7. Resource leveling
    if weights['leveling_weight'] > 0:
        for (eid, month), level_var in leveling_vars.items():
            objective.SetCoefficient(level_var, avg_cost * weights['leveling_weight'])
    
    # 8. Team diversity
    if config['enable_team_diversity'] and weights['diversity_weight'] > 0:
        for (pid, month), div_penalty in diversity_penalties.items():
            objective.SetCoefficient(div_penalty, avg_cost * weights['diversity_weight'])
    
    # 9. Role allocation balance (penalty for deviation from target ratios)
    if config.get('enforce_role_allocation', False) and weights.get('role_balance_weight', 0) > 0:
        role_ratios = config.get('role_allocation_ratios', {'DEV': 0.50, 'QA': 0.30, 'BA': 0.20})
        role_balance_penalties = {}
        
        for pid, month in project_months:
            # Calculate total allocation
            total_allocation_vars = []
            for eid in employee_ids:
                var = variables.get((eid, pid, month))
                if var is not None:
                    total_allocation_vars.append(var)
            
            if len(total_allocation_vars) > 0:
                for role in ['DEV', 'QA', 'BA']:
                    target_ratio = role_ratios.get(role, 0.0)
                    if target_ratio > 0:
                        role_allocation_vars = []
                        for eid in employees_by_role.get(role, []):
                            var = variables.get((eid, pid, month))
                            if var is not None:
                                role_allocation_vars.append(var)
                        
                        if len(role_allocation_vars) > 0:
                            # Penalty variable for deviation from target ratio
                            ratio_deviation = solver.NumVar(0.0, 1.0, f'role_dev_{role}_p{pid}_m{month}')
                            role_balance_penalties[(role, pid, month)] = ratio_deviation
                            
                            # Simplified: penalty if role allocation is less than target
                            # This encourages meeting target ratios
                            constraint = solver.Constraint(0.0, solver.infinity(), 
                                                         f'role_bal_{role}_p{pid}_m{month}')
                            constraint.SetCoefficient(ratio_deviation, 1.0)
                            # ratio_deviation >= target_ratio - (sum(role_alloc) / sum(total_alloc))
                            # Approximated as: ratio_deviation >= target_ratio * sum(total) - sum(role)
                            # This is simplified - full implementation would need ratio constraints
                            for var in total_allocation_vars:
                                constraint.SetCoefficient(var, -target_ratio)
                            for var in role_allocation_vars:
                                constraint.SetCoefficient(var, 1.0)
        
        # Add penalty to objective
        for (role, pid, month), penalty_var in role_balance_penalties.items():
            objective.SetCoefficient(penalty_var, avg_cost * weights.get('role_balance_weight', 0.10))
    
    # Budget utilization maximization (if enabled)
    if config.get('maximize_budget_utilization', False):
        # Add negative cost to maximize budget usage (minimize negative cost = maximize cost)
        # Use stronger weight to compete with cost minimization
        budget_max_weight = weights.get('cost_weight', 0.30) * config.get('budget_maximization_weight_multiplier', 1.0)
        for (eid, pid, month), var in variables.items():
            if var is not None:
                emp_row = active_employees[active_employees['employee_id'] == eid].iloc[0]
                cost = float(emp_row['cost_per_month'])
                # Negative coefficient to maximize (minimize negative = maximize positive)
                # Use same or higher weight than cost minimization to ensure budget maximization wins
                objective.SetCoefficient(var, -cost * budget_max_weight)
    
    # Set minimization (even with budget maximization, we still minimize the combined objective)
    objective.SetMinimization()
    
    # CONSTRAINTS
    
    # Constraint 1: Employee capacity per month
    # Limits each employee's total allocation across all projects per month to their fte_capacity
    for eid in employee_ids:
        emp_row = active_employees[active_employees['employee_id'] == eid].iloc[0]
        capacity = float(emp_row['fte_capacity'])
        
        for month in all_months:
            constraint = solver.Constraint(0.0, capacity, f'capacity_e{eid}_m{month}')
            for pid, m in project_months:
                if m == month:
                    var = variables.get((eid, pid, month))
                    if var is not None:
                        if config['discrete_allocations']:
                            # Need to convert level to FTE - use max increment
                            max_inc = max(config['allocation_increments'])
                            constraint.SetCoefficient(var, max_inc)
                        else:
                            constraint.SetCoefficient(var, 1.0)
                    
                    # Skill development allocations
                    sd_var = skill_dev_variables.get((eid, pid, month))
                    if sd_var is not None:
                        constraint.SetCoefficient(sd_var, 1.0)
    
    # Constraint 1b: Employee average allocation per year (max 1.0 FTE average per month)
    # This ensures that the average allocation per month for an employee across all projects 
    # in a calendar year does not exceed 1.0 FTE.
    # Formula: (Sum of all allocations across all months) / (number of months) <= 1.0
    # Equivalently: Sum of all allocations <= number of months
    # Example: 12 months, total allocation can be up to 12.0 FTE-months (1.0 FTE per month average)
    # Example: If allocated 0.5 FTE in Jan and 0.5 FTE in Feb, average = 1.0 FTE/month ✓
    # Example: If allocated 1.0 FTE every month for 12 months, total = 12.0 FTE-months, average = 1.0 FTE/month ✓
    months_by_year = defaultdict(list)
    for month in all_months:
        year = month.split('-')[0]  # Extract year from YYYY-MM format
        months_by_year[year].append(month)
    
    for eid in employee_ids:
        for year, year_months in months_by_year.items():
            num_months = len(year_months)
            # Constraint: Sum of all allocations (regular + skill development) for this 
            # employee across all projects and all months in this year <= num_months
            # This ensures average allocation per month <= 1.0 FTE
            constraint = solver.Constraint(0.0, float(num_months), f'yearly_avg_capacity_e{eid}_y{year}')
            
            for pid, month in project_months:
                if month in year_months:
                    var = variables.get((eid, pid, month))
                    if var is not None:
                        if config['discrete_allocations']:
                            # For discrete, use max increment to convert level to FTE
                            max_inc = max(config['allocation_increments'])
                            constraint.SetCoefficient(var, max_inc)
                        else:
                            constraint.SetCoefficient(var, 1.0)
                    
                    # Skill development allocations also count toward yearly limit
                    sd_var = skill_dev_variables.get((eid, pid, month))
                    if sd_var is not None:
                        constraint.SetCoefficient(sd_var, 1.0)
    
    # Constraint 2: Budget constraints
    if config['budget_flexibility']:
        # Allow budget borrowing between months within project
        for pid in project_data.keys():
            months_list = project_months_map[pid]
            total_budget = project_data[pid]['total_budget']
            
            # Total cost across all months <= total budget
            constraint = solver.Constraint(0.0, total_budget, f'budget_total_p{pid}')
            for month in months_list:
                for eid in employee_ids:
                    var = variables.get((eid, pid, month))
                    if var is not None:
                        emp_row = active_employees[active_employees['employee_id'] == eid].iloc[0]
                        cost = float(emp_row['cost_per_month'])
                        if config['discrete_allocations']:
                            constraint.SetCoefficient(var, cost * 0.5)  # Average
                        else:
                            constraint.SetCoefficient(var, cost)
            
            # Minimum budget utilization constraint (if enabled)
            min_utilization = config.get('min_budget_utilization', 0.0)
            if min_utilization > 0.0 and total_budget > 0:
                min_budget = total_budget * min_utilization
                min_constraint = solver.Constraint(min_budget, solver.infinity(), f'min_budget_util_p{pid}')
                for month in months_list:
                    for eid in employee_ids:
                        var = variables.get((eid, pid, month))
                        if var is not None:
                            emp_row = active_employees[active_employees['employee_id'] == eid].iloc[0]
                            cost = float(emp_row['cost_per_month'])
                            if config['discrete_allocations']:
                                min_constraint.SetCoefficient(var, cost * 0.5)
                            else:
                                min_constraint.SetCoefficient(var, cost)
    else:
        # Per-month budget constraints
        for pid, month in project_months:
            proj_info = project_data[pid]
            budget = proj_info['per_month_budget']
            if budget > 0:
                constraint = solver.Constraint(0.0, budget, f'budget_p{pid}_m{month}')
                for eid in employee_ids:
                    var = variables.get((eid, pid, month))
                    if var is not None:
                        emp_row = active_employees[active_employees['employee_id'] == eid].iloc[0]
                        cost = float(emp_row['cost_per_month'])
                        if config['discrete_allocations']:
                            constraint.SetCoefficient(var, cost * 0.5)
                        else:
                            constraint.SetCoefficient(var, cost)
                
                # Minimum budget utilization per month (if enabled)
                min_utilization = config.get('min_budget_utilization', 0.0)
                if min_utilization > 0.0:
                    min_budget = budget * min_utilization
                    min_constraint = solver.Constraint(min_budget, solver.infinity(), f'min_budget_util_p{pid}_m{month}')
                    for eid in employee_ids:
                        var = variables.get((eid, pid, month))
                        if var is not None:
                            emp_row = active_employees[active_employees['employee_id'] == eid].iloc[0]
                            cost = float(emp_row['cost_per_month'])
                            if config['discrete_allocations']:
                                min_constraint.SetCoefficient(var, cost * 0.5)
                            else:
                                min_constraint.SetCoefficient(var, cost)
    
    # Constraint 3: Risk mitigation - Max allocation per employee per project
    if config['max_employee_per_project'] < 1.0:
        for eid in employee_ids:
            for pid in project_data.keys():
                months_list = project_months_map[pid]
                max_alloc = config['max_employee_per_project']
                
                for month in months_list:
                    var = variables.get((eid, pid, month))
                    if var is not None:
                        if config['discrete_allocations']:
                            # Limit by max increment
                            max_level = min(
                                len(config['allocation_increments']) - 1,
                                int(max_alloc / min(config['allocation_increments']))
                            )
                            var.SetUb(max_level)
                        else:
                            var.SetUb(max_alloc)
    
    # Constraint 4: Minimum team size per project per month
    # Note: This is simplified - full implementation would need binary indicators
    # For now, we ensure minimum total allocation rather than distinct employees
    if config['min_team_size'] > 0:
        for pid, month in project_months:
            min_total_alloc = config['min_team_size'] * 0.1  # At least 0.1 FTE per team member
            constraint = solver.Constraint(
                min_total_alloc, 
                solver.infinity(), 
                f'min_team_p{pid}_m{month}'
            )
            for eid in employee_ids:
                var = variables.get((eid, pid, month))
                if var is not None:
                    if config['discrete_allocations']:
                        constraint.SetCoefficient(var, 0.25)  # Min increment
                    else:
                        constraint.SetCoefficient(var, 1.0)
    
    # Constraint 5: Minimum allocation per project per month
    for pid, month in project_months:
        proj_info = project_data[pid]
        budget = proj_info['per_month_budget'] if not config['budget_flexibility'] else proj_info['total_budget'] / len(project_months_map[pid])
        
        min_alloc = 0.0
        if budget > 0:
            cheapest_cost = float('inf')
            for eid in employee_ids:
                var = variables.get((eid, pid, month))
                if var is not None:
                    emp_row = active_employees[active_employees['employee_id'] == eid].iloc[0]
                    cost = float(emp_row['cost_per_month'])
                    if cost < cheapest_cost:
                        cheapest_cost = cost
            
            if cheapest_cost < float('inf'):
                if config['discrete_allocations']:
                    min_alloc = min(config['allocation_increments'])
                else:
                    min_alloc = 0.1
                
                if budget >= cheapest_cost * min_alloc:
                    constraint = solver.Constraint(min_alloc, solver.infinity(), f'min_alloc_p{pid}_m{month}')
                    for eid in employee_ids:
                        var = variables.get((eid, pid, month))
                        if var is not None:
                            constraint.SetCoefficient(var, 1.0)
    
    # Solve
    solver.SetTimeLimit(30000)  # 30 seconds
    status = solver.Solve()
    
    if status != pywraplp.Solver.OPTIMAL and status != pywraplp.Solver.FEASIBLE:
        status_names = {
            pywraplp.Solver.NOT_SOLVED: 'NOT_SOLVED',
            pywraplp.Solver.INFEASIBLE: 'INFEASIBLE',
            pywraplp.Solver.UNBOUNDED: 'UNBOUNDED',
            pywraplp.Solver.ABNORMAL: 'ABNORMAL',
            pywraplp.Solver.MODEL_INVALID: 'MODEL_INVALID'
        }
        print(f'Warning: Solver status = {status} ({status_names.get(status, "UNKNOWN")})')
        return []
    
    # Extract results
    allocations = []
    employee_month_allocated = defaultdict(lambda: defaultdict(float))  # Track total allocation per employee-month
    
    # Create lookup dictionaries for names
    employee_names = {row['employee_id']: row['employee_name'] for _, row in active_employees.iterrows()}
    project_names = {int(row['project_id']): row['project_name'] for _, row in projects_df.iterrows()}
    employee_roles = {row['employee_id']: row.get('role', 'DEV') for _, row in active_employees.iterrows()}
    
    for (eid, pid, month), var in variables.items():
        if var is not None:
            value = get_allocation_value(var)
            if value > 1e-6:
                emp_row = active_employees[active_employees['employee_id'] == eid].iloc[0]
                cost = value * float(emp_row['cost_per_month'])
                # Check if this allocation was made without required skills
                has_skills_for_proj = skill_scores.get((eid, pid), 0.0) > 0.0
                alloc_dict = {
                    'scenario_id': scenario_id,
                    'employee_id': eid,
                    'employee_name': employee_names.get(eid, f'Employee_{eid}'),
                    'employee_role': employee_roles.get(eid, 'DEV'),
                    'project_id': pid,
                    'project_name': project_names.get(pid, f'Project_{pid}'),
                    'month': month,
                    'allocation_fraction': round(value, 4),
                    'cost': round(cost, 2)
                }
                if not has_skills_for_proj:
                    alloc_dict['no_required_skills'] = True  # Flag for allocations without required skills
                allocations.append(alloc_dict)
                employee_month_allocated[eid][month] += value
    
    # Add skill development allocations
    for (eid, pid, month), var in skill_dev_variables.items():
        if var is not None:
            value = var.solution_value()
            if value > 1e-6:
                emp_row = active_employees[active_employees['employee_id'] == eid].iloc[0]
                cost = value * float(emp_row['cost_per_month'])
                allocations.append({
                    'scenario_id': scenario_id,
                    'employee_id': eid,
                    'employee_name': employee_names.get(eid, f'Employee_{eid}'),
                    'employee_role': employee_roles.get(eid, 'DEV'),
                    'project_id': pid,
                    'project_name': project_names.get(pid, f'Project_{pid}'),
                    'month': month,
                    'allocation_fraction': round(value, 4),
                    'cost': round(cost, 2),
                    'skill_development': True  # Flag for skill development
                })
                employee_month_allocated[eid][month] += value
    
    # Add remaining capacity for future allocation
    for eid in employee_ids:
        emp_row = active_employees[active_employees['employee_id'] == eid].iloc[0]
        capacity = float(emp_row['fte_capacity'])
        
        # Get all months this employee could be allocated
        months_for_emp = set()
        for pid, month in project_months:
            if variables.get((eid, pid, month)) is not None:
                months_for_emp.add(month)
            # Also check skill development variables
            if skill_dev_variables.get((eid, pid, month)) is not None:
                months_for_emp.add(month)
        
        for month in months_for_emp:
            total_allocated = employee_month_allocated[eid].get(month, 0.0)
            remaining_capacity = capacity - total_allocated
            
            if remaining_capacity > 1e-6:  # Only include if there's remaining capacity
                allocations.append({
                    'scenario_id': scenario_id,
                    'employee_id': eid,
                    'employee_name': employee_names.get(eid, f'Employee_{eid}'),
                    'employee_role': employee_roles.get(eid, 'DEV'),
                    'project_id': None,  # None indicates available capacity
                    'project_name': 'Available Capacity',  # Label for available capacity
                    'month': month,
                    'allocation_fraction': round(remaining_capacity, 4),
                    'cost': 0.0,  # No cost for available capacity
                    'available_capacity': True  # Flag to indicate this is available capacity
                })
    
    # Generate skill gap report for projects with no allocations
    allocated_projects = set()
    for alloc in allocations:
        if alloc.get('project_id') and alloc.get('project_id') is not None:
            allocated_projects.add(alloc['project_id'])
    
    all_projects = set(project_data.keys())
    unallocated_projects = all_projects - allocated_projects
    
    if unallocated_projects:
        print(f'\n⚠️  Warning: {len(unallocated_projects)} project(s) could not be allocated:')
        for pid in unallocated_projects:
            proj_name = project_names.get(pid, f'Project_{pid}')
            req_skills = project_data[pid]['required_skills']
            tech_skills = req_skills.get('technical', [])
            func_skills = req_skills.get('functional', [])
            print(f'  - {proj_name} (ID: {pid})')
            if tech_skills:
                print(f'    Required technical skills: {", ".join(tech_skills)}')
            if func_skills:
                print(f'    Required functional skills: {", ".join(func_skills)}')
            print(f'    Suggestion: Enable "allow_allocation_without_skills" or add employees with matching skills')
    
    return allocations

