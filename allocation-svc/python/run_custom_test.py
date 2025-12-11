"""run_custom_test.py - Custom test with specific projects and employees"""
import pandas as pd
import json
import sys
from pathlib import Path
from allocate_fully_optimized import fully_optimized_allocator
from config_loader import get_config, get_weights

BASE = Path(__file__).resolve().parent
out_path = BASE.parent / 'excel' / 'custom_test_allocations.xlsx'

# Create employees DataFrame
employees = pd.DataFrame([
    {
        'employee_id': 1,
        'employee_name': 'Pooja',
        'status': 'active',
        'grade': 'G5',
        'location': 'Bengaluru',
        'country': 'India',
        'gender': 'female',
        'technical_skills': 'dev,java',
        'functional_skills': 'delta1',
        'cost_per_month': 3267,
        'region': 'IN',
        'fte_capacity': 1.0,
        'role': 'DEV'
    },
    {
        'employee_id': 2,
        'employee_name': 'Vikas Dubey',
        'status': 'active',
        'grade': 'G5',
        'location': 'Bengaluru',
        'country': 'India',
        'gender': 'male',
        'technical_skills': 'qa,testing',
        'functional_skills': 'delta1',
        'cost_per_month': 3267,
        'region': 'IN',
        'fte_capacity': 1.0,
        'role': 'QA'
    },
    {
        'employee_id': 3,
        'employee_name': 'Birumandandan',
        'status': 'active',
        'grade': 'G6',
        'location': 'Bengaluru',
        'country': 'India',
        'gender': 'male',
        'technical_skills': 'core-lifecycle',
        'functional_skills': 'core-lifecycle,delta1',
        'cost_per_month': 12970,  # Default since not specified
        'region': 'IN',
        'fte_capacity': 1.0,
        'role': 'BA'
    },
    # Adding 5 more employees to make 8 total
    {
        'employee_id': 4,
        'employee_name': 'Atul',
        'status': 'active',
        'grade': 'G5',
        'location': 'Bengaluru',
        'country': 'India',
        'gender': 'other',
        'technical_skills': 'dev,java',
        'functional_skills': 'core-lifecycle,delta1,risk',
        'cost_per_month': 3267,
        'region': 'IN',
        'fte_capacity': 1.0,
        'role': 'DEV'
    },
    {
        'employee_id': 5,
        'employee_name': 'Sagar ',
        'status': 'active',
        'grade': 'G5',
        'location': 'Bengaluru',
        'country': 'India',
        'gender': 'other',
        'technical_skills': 'ETL,SQL,Informatica',
        'functional_skills': 'swapmart,cashbalance',
        'cost_per_month': 3267,
        'region': 'IN',
        'fte_capacity': 1.0,
        'role': 'QA'
    },
    {
        'employee_id': 6,
        'employee_name': 'Kushal ',
        'status': 'active',
        'grade': 'G6',
        'location': 'Bengaluru',
        'country': 'India',
        'gender': 'other',
        'technical_skills': 'dev,java',
        'functional_skills': 'core-lifecycle,delta1,interest',
        'cost_per_month': 3500,
        'region': 'IN',
        'fte_capacity': 1.0,
        'role': 'BA'
    },
    {
        'employee_id': 7,
        'employee_name': 'shivani ',
        'status': 'active',
        'grade': 'G5',
        'location': 'Bengaluru',
        'country': 'India',
        'gender': 'other',
        'technical_skills': 'dev,java',
        'functional_skills': 'core-lifecycle,delta1',
        'cost_per_month': 3267,
        'region': 'IN',
        'fte_capacity': 1.0,
        'role': 'DEV'
    },
    {
        'employee_id': 8,
        'employee_name': 'Employee 8',
        'status': 'active',
        'grade': 'G5',
        'location': 'Bengaluru',
        'country': 'India',
        'gender': 'other',
        'technical_skills': 'java',
        'functional_skills': 'synfiny',
        'cost_per_month': 3267,
        'region': 'IN',
        'fte_capacity': 1.0,
        'role': 'QA'
    },
    # Add an employee without required skills to test the marker
    {
        'employee_id': 9,
        'employee_name': 'Employee 9 (No Skills)',
        'status': 'active',
        'grade': 'G5',
        'location': 'Bengaluru',
        'country': 'India',
        'gender': 'other',
        'technical_skills': 'python',  # Different tech skill
        'functional_skills': 'other',  # Different functional skill
        'cost_per_month': 3000,
        'region': 'IN',
        'fte_capacity': 1.0,
        'role': 'DEV'
    }
])

# Create projects DataFrame
projects = pd.DataFrame([
    {
        'project_id': 1,
        'project_name': 'actual vs contractual',
        'funding_source': 'Internal',
        'project_driver': 'Product',
        'stakeholders': '',
        'impact': 'High',
        'metrics': 'quality',
        'comments': 'Max budget 160000 (extended to 2026-12, 8x for 24 months)',
        'max_budget': 160000,  # Increased 8x for 24 months (was 20000 for 3 months)
        'region_preference': '',
        'required_skills': json.dumps({
            'technical': ['java'],
            'functional': ['core-lifecycle', 'delta1']
        }),
        'start_month': '2026-01',
        'end_month': '2026-12'
    },
    {
        'project_id': 2,
        'project_name': 'Autoreset',
        'funding_source': 'Internal',
        'project_driver': 'Product',
        'stakeholders': '',
        'impact': 'High',
        'metrics': 'quality',
        'comments': 'Max budget 80000 (extended to 2026-12, 8x for 24 months)',
        'max_budget': 80000,  # Increased 8x for 24 months (was 10000 for 3 months)
        'region_preference': '',
        'required_skills': json.dumps({
            'technical': ['java'],
            'functional': ['core-lifecycle', 'delta1']
        }),
        'start_month': '2026-01',
        'end_month': '2026-12'
    },
    {
        'project_id': 3,
        'project_name': 'L3 support',
        'funding_source': 'Internal',
        'project_driver': 'Product',
        'stakeholders': '',
        'impact': 'High',
        'metrics': 'quality',
        'comments': 'Cost 3692696 (extended to 2026-12, 8x for 24 months)',
        'max_budget': 3692696,  # Increased 8x for 24 months (was 461587 for 3 months)
        'region_preference': '',
        'required_skills': json.dumps({
            'technical': ['java'],
            'functional': ['core-lifecycle', 'delta1']
        }),
        'start_month': '2026-01',
        'end_month': '2026-12'
    }
])

print("=" * 80)
print("CUSTOM TEST - Projects and Employees")
print("=" * 80)
print(f"\nEmployees ({len(employees)}):")
for _, emp in employees.iterrows():
    print(f"  {emp['employee_id']}. {emp['employee_name']} ({emp['role']}) - "
          f"Skills: {emp['technical_skills']}, {emp['functional_skills']} - "
          f"Cost: ${emp['cost_per_month']}/month")

print(f"\nProjects ({len(projects)}):")
for _, proj in projects.iterrows():
    req_skills = json.loads(proj['required_skills'])
    print(f"  {proj['project_id']}. {proj['project_name']} - "
          f"Budget: ${proj['max_budget']:,} - "
          f"Skills: {req_skills['technical']}, {req_skills['functional']}")

# Run allocator with budget maximization
scenario_id = 1
global_start = projects['start_month'].min()
global_end = projects['end_month'].max()

# Configure to maximize budget utilization and allow allocation without skills
# For extended timeline, we may need to relax some constraints
# Load configuration from file or use inline defaults
# Priority: 1) Command line arg, 2) allocator_config.json/yaml, 3) inline defaults
config_path = sys.argv[1] if len(sys.argv) > 1 else None
try:
    config = get_config(config_path, validate=True)
    if config:
        print(f"✓ Loaded configuration from file")
    else:
        print("ℹ Using default configuration (no config file found)")
        # Fallback to inline defaults
        config = {
            'maximize_budget_utilization': True,
            'budget_maximization_weight_multiplier': 50.0,  # Very strongly prioritize budget utilization
            'min_budget_utilization': 0.0,
            'allow_allocation_without_skills': True,
            'no_skills_penalty_multiplier': 1.0,  # Minimal penalty - just slight preference for skills
            'min_team_size': 0,
            'max_employee_per_project': 1.0,  # Allow full allocation to projects (100% of capacity)
            'enforce_role_allocation': False,  # Disable role constraints to allow more flexible allocation
        }
except Exception as e:
    print(f"⚠ Warning: Could not load config file: {e}")
    print("   Using inline default configuration")
    config = {
        'maximize_budget_utilization': True,
        'budget_maximization_weight_multiplier': 1.5,
        'min_budget_utilization': 0.0,
        'allow_allocation_without_skills': True,
        'no_skills_penalty_multiplier': 2.0,
        'min_team_size': 0,
    }

print(f"\nRunning allocator for period: {global_start} to {global_end}...")
print("Configuration: maximize_budget_utilization = True")

# Load weights from config if present, otherwise use defaults optimized for budget maximization
custom_weights = get_weights(config)
if custom_weights is None:
    # Default weights to prioritize budget utilization - minimize all penalty terms
    custom_weights = {
        'cost_weight': 0.0,  # Disabled - budget maximization handles this
        'skill_weight': 0.0,  # Disabled - allow allocations without perfect skill matches
        'fragmentation_weight': 0.0,  # Disabled
        'continuity_weight': 0.0,  # Disabled
        'balance_weight': 0.0,  # Disabled - allow full allocation to employees
        'preference_weight': 0.0,  # Disabled
        'diversity_weight': 0.0,  # Disabled
        'leveling_weight': 0.0,  # Disabled
        'role_balance_weight': 0.0  # Disabled since enforce_role_allocation is False
    }
    print("ℹ Using default weights optimized for budget maximization")
else:
    print("✓ Loaded weights from configuration")

allocs = fully_optimized_allocator(
    employees, projects, scenario_id,
    global_start=global_start, global_end=global_end,
    config=config,
    weights=custom_weights
)

if not allocs or len(allocs) == 0:
    print('\n❌ ERROR: Solver returned INFEASIBLE or no allocations')
    print('   This means the constraints cannot be satisfied.')
    print('   Possible reasons:')
    print('   - Budgets too small for the extended timeline')
    print('   - Minimum allocation constraints too restrictive')
    print('   - Employee capacity insufficient')
    print('\n   Try:')
    print('   - Increasing project budgets')
    print('   - Reducing min_budget_utilization')
    print('   - Adjusting minimum allocation constraints')
    exit(1)

allocs_df = pd.DataFrame(allocs)

# Separate actual allocations from available capacity
actual_allocations = allocs_df[allocs_df['project_id'].notna()].copy()
# Check if available_capacity column exists (it might not be in all allocator outputs)
if 'available_capacity' in allocs_df.columns:
    available_capacity = allocs_df[(allocs_df['available_capacity'] == True) | (allocs_df['project_id'].isna())].copy()
else:
    available_capacity = allocs_df[allocs_df['project_id'].isna()].copy()

# For employee utilization calculation, we need ALL allocations (projects + available capacity)
# to get the true total utilization per month
all_allocations_for_util = allocs_df.copy()  # Use all allocations for utilization calculation

print(f'\nAllocation Summary:')
print(f'  Actual allocations: {len(actual_allocations)}')
print(f'  Total cost: ${actual_allocations["cost"].sum():,.2f}')
print(f'  Available capacity records: {len(available_capacity)}')
if len(available_capacity) > 0:
    print(f'  Total available FTE: {available_capacity["allocation_fraction"].sum():.2f}')

# Check for allocations without required skills
no_skills_allocations = pd.DataFrame()
if len(actual_allocations) > 0:
    # Add marker column if it doesn't exist (for display)
    if 'no_required_skills' not in actual_allocations.columns:
        actual_allocations['no_required_skills'] = False
    if 'no_required_skills' in actual_allocations.columns:
        no_skills_allocations = actual_allocations[actual_allocations['no_required_skills'] == True].copy()
        if len(no_skills_allocations) > 0:
            print(f'\n⚠️  Warning: {len(no_skills_allocations)} allocation(s) made without required skills')
            print(f'  Total cost of no-skills allocations: ${no_skills_allocations["cost"].sum():,.2f}')
            print(f'  These allocations are marked in the Excel output with "no_required_skills = True"')
    
    # Add a clear marker column for Excel display
    actual_allocations['Allocation_Type'] = actual_allocations.apply(
        lambda row: 'WITHOUT REQUIRED SKILLS ⚠️' if row.get('no_required_skills', False) 
        else ('SKILL DEVELOPMENT 📚' if row.get('skill_development', False) 
        else 'NORMAL'), axis=1
    )

# Check for skill development allocations
skill_dev_allocations = pd.DataFrame()
if 'skill_development' in actual_allocations.columns:
    skill_dev_allocations = actual_allocations[actual_allocations['skill_development'] == True].copy()
    if len(skill_dev_allocations) > 0:
        print(f'\n📚 Skill Development: {len(skill_dev_allocations)} allocation(s) for skill development')

def generate_variance_explanations(projects, employees, actual_allocations, config):
    """Generate explanations for why projects and employees are not fully allocated."""
    import json
    
    # Calculate project explanations
    project_explanations = []
    if len(actual_allocations) > 0:
        project_costs = actual_allocations.groupby('project_id')['cost'].sum()
        project_fte = actual_allocations.groupby('project_id')['allocation_fraction'].sum()
        project_employees = actual_allocations.groupby('project_id')['employee_id'].nunique()
    else:
        project_costs = pd.Series(dtype=float)
        project_fte = pd.Series(dtype=float)
        project_employees = pd.Series(dtype=int)
    
    # Calculate total employee capacity
    total_employee_capacity = employees['fte_capacity'].sum()
    total_employee_cost = employees['cost_per_month'].mean() * total_employee_capacity
    
    for _, proj in projects.iterrows():
        pid = proj['project_id']
        max_budget = float(proj['max_budget'])
        allocated_cost = project_costs.get(pid, 0.0)
        allocated_fte = project_fte.get(pid, 0.0)
        num_employees = project_employees.get(pid, 0)
        utilization = (allocated_cost / max_budget * 100) if max_budget > 0 else 0
        
        reasons = []
        
        # Check if under-allocated
        if utilization < 95.0:  # Consider <95% as under-allocated
            # Reason 1: Budget too large
            months = pd.date_range(
                start=proj['start_month'] + "-01", 
                end=proj['end_month'] + "-01", 
                freq='MS'
            ).strftime('%Y-%m').tolist()
            num_months = len(months)
            max_possible_monthly = total_employee_cost / num_months if num_months > 0 else total_employee_cost
            per_month_budget = max_budget / num_months if num_months > 0 else max_budget
            
            if per_month_budget > max_possible_monthly * 1.2:  # 20% buffer
                reasons.append(f"Budget too large: ${per_month_budget:,.0f}/month exceeds max possible ${max_possible_monthly:,.0f}/month")
            
            # Reason 2: Not enough employees with matching skills
            req_skills = json.loads(proj.get('required_skills', '{}'))
            matching_employees = 0
            if req_skills:
                tech_req = req_skills.get('technical', [])
                func_req = req_skills.get('functional', [])
                for _, emp in employees.iterrows():
                    emp_tech = str(emp.get('technical_skills', '')).lower()
                    emp_func = str(emp.get('functional_skills', '')).lower()
                    has_match = False
                    if tech_req:
                        has_match = any(t.lower() in emp_tech for t in tech_req)
                    if not has_match and func_req:
                        has_match = any(f.lower() in emp_func for f in func_req)
                    if has_match:
                        matching_employees += 1
            
            if matching_employees == 0 and not config.get('allow_allocation_without_skills', False):
                reasons.append(f"No employees with required skills (tech: {req_skills.get('technical', [])}, func: {req_skills.get('functional', [])})")
            elif matching_employees < 2:
                reasons.append(f"Only {matching_employees} employee(s) with matching skills (may limit allocation)")
            
            # Reason 3: Employee capacity constraints
            total_allocated_fte = actual_allocations['allocation_fraction'].sum() if len(actual_allocations) > 0 else 0
            if total_allocated_fte > total_employee_capacity * 0.9:
                reasons.append(f"Employee capacity nearly exhausted ({total_allocated_fte:.1f}/{total_employee_capacity:.1f} FTE used)")
            
            # Reason 4: max_employee_per_project limit
            max_per_emp = config.get('max_employee_per_project', 0.8)
            if max_per_emp < 1.0:
                max_possible_per_project = max_per_emp * matching_employees * num_months
                if allocated_fte < max_possible_per_project * 0.8:
                    reasons.append(f"max_employee_per_project={max_per_emp} limits allocation (only {num_employees} employees allocated)")
            
            # Reason 5: Budget maximization not enabled or too weak
            if not config.get('maximize_budget_utilization', False):
                reasons.append("Budget maximization disabled (cost minimization prioritized)")
            elif config.get('budget_maximization_weight_multiplier', 1.0) < 1.0:
                reasons.append(f"Budget maximization weight too low (multiplier={config.get('budget_maximization_weight_multiplier', 1.0)})")
            
            # Reason 6: Role allocation constraints
            if config.get('enforce_role_allocation', False):
                min_role = config.get('min_role_allocation', {})
                if any(v > 0 for v in min_role.values()):
                    reasons.append(f"Role allocation constraints may limit allocation (min_role_allocation={min_role})")
        
        explanation = "; ".join(reasons) if reasons else "Fully allocated or within acceptable variance"
        project_explanations.append(explanation)
    
    # Calculate employee explanations
    employee_explanations = []
    if len(actual_allocations) > 0:
        employee_costs = actual_allocations.groupby('employee_id')['cost'].sum()
        employee_fte = actual_allocations.groupby('employee_id')['allocation_fraction'].sum()
        employee_projects = actual_allocations.groupby('employee_id')['project_id'].nunique()
    else:
        employee_costs = pd.Series(dtype=float)
        employee_fte = pd.Series(dtype=float)
        employee_projects = pd.Series(dtype=int)
    
    for _, emp in employees.iterrows():
        eid = emp['employee_id']
        capacity = float(emp['fte_capacity'])
        allocated_fte = employee_fte.get(eid, 0.0)
        utilization = (allocated_fte / capacity * 100) if capacity > 0 else 0
        num_projects = employee_projects.get(eid, 0)
        
        reasons = []
        
        # Check if under-utilized
        if utilization < 90.0:  # Consider <90% as under-utilized
            # Reason 1: No matching projects
            emp_tech = str(emp.get('technical_skills', '')).lower()
            emp_func = str(emp.get('functional_skills', '')).lower()
            matching_projects = 0
            for _, proj in projects.iterrows():
                req_skills = json.loads(proj.get('required_skills', '{}'))
                tech_req = req_skills.get('technical', [])
                func_req = req_skills.get('functional', [])
                has_match = False
                if tech_req:
                    has_match = any(t.lower() in emp_tech for t in tech_req)
                if not has_match and func_req:
                    has_match = any(f.lower() in emp_func for f in func_req)
                if has_match or config.get('allow_allocation_without_skills', False):
                    matching_projects += 1
            
            if matching_projects == 0:
                reasons.append("No projects with matching skills and allow_allocation_without_skills=False")
            elif num_projects == 0:
                reasons.append(f"{matching_projects} matching project(s) available but not allocated (may be cost/constraint limited)")
            
            # Reason 2: max_employee_per_project limit reached
            max_per_emp = config.get('max_employee_per_project', 0.8)
            if num_projects > 0 and allocated_fte < capacity * max_per_emp:
                reasons.append(f"max_employee_per_project={max_per_emp} limits allocation across projects")
            
            # Reason 3: Cost too high
            emp_cost = float(emp.get('cost_per_month', 0))
            avg_cost = employees['cost_per_month'].mean()
            if emp_cost > avg_cost * 1.5:
                reasons.append(f"Cost above average (${emp_cost:,.0f} vs ${avg_cost:,.0f}/month) - cost minimization may prefer cheaper employees")
            
            # Reason 4: All projects fully allocated
            if num_projects > 0:
                # Check if allocated projects are at capacity
                proj_utilizations = []
                for pid in actual_allocations[actual_allocations['employee_id'] == eid]['project_id'].unique():
                    proj_row = projects[projects['project_id'] == pid]
                    if len(proj_row) > 0:
                        proj_budget = float(proj_row.iloc[0]['max_budget'])
                        proj_allocated = project_costs.get(pid, 0.0)
                        proj_util = (proj_allocated / proj_budget * 100) if proj_budget > 0 else 0
                        proj_utilizations.append(proj_util)
                if all(u > 95 for u in proj_utilizations):
                    reasons.append("All allocated projects are fully utilized")
        
        explanation = "; ".join(reasons) if reasons else "Fully utilized or within acceptable variance"
        employee_explanations.append(explanation)
    
    return project_explanations, employee_explanations

# Generate variance explanations
print('\nGenerating variance explanations...')
project_explanations, employee_explanations = generate_variance_explanations(
    projects, employees, actual_allocations, config
)

# Create pivot views
from run_demo import create_pivot_views
print('\nCreating pivot views...')
monthly_pivot, quarterly_pivot = create_pivot_views(allocs_df)
print(f'  Monthly pivot: {len(monthly_pivot)} rows')
print(f'  Quarterly pivot: {len(quarterly_pivot)} rows')

# Write to Excel
print(f'\nWriting to Excel: {out_path}')
with pd.ExcelWriter(str(out_path), engine='openpyxl') as writer:
    # Original format sheets
    allocs_df.to_excel(writer, sheet_name='Allocations', index=False)
    if len(actual_allocations) > 0:
        actual_allocations.to_excel(writer, sheet_name='Project_Allocations', index=False)
    if len(available_capacity) > 0:
        available_capacity.to_excel(writer, sheet_name='Available_Capacity', index=False)
    
    # Pivot format sheets
    if len(monthly_pivot) > 0:
        monthly_pivot.to_excel(writer, sheet_name='Monthly_View', index=False)
        print('  ✓ Created Monthly_View sheet')
    if len(quarterly_pivot) > 0:
        quarterly_pivot.to_excel(writer, sheet_name='Quarterly_View', index=False)
        print('  ✓ Created Quarterly_View sheet')
    
    # Skill gap reporting
    if len(no_skills_allocations) > 0:
        no_skills_allocations.to_excel(writer, sheet_name='No_Skills_Allocations', index=False)
        print('  ✓ Created No_Skills_Allocations sheet (allocations without required skills)')
    if len(skill_dev_allocations) > 0:
        skill_dev_allocations.to_excel(writer, sheet_name='Skill_Development', index=False)
        print('  ✓ Created Skill_Development sheet (skill development allocations)')
    
    # Add budget utilization to Projects sheet
    projects_with_budget = projects.copy()
    if len(actual_allocations) > 0:
        # Calculate allocated cost per project
        project_costs = actual_allocations.groupby('project_id')['cost'].sum().reset_index()
        project_costs.columns = ['project_id', 'allocated_cost']
        
        # Merge with projects
        projects_with_budget = projects_with_budget.merge(
            project_costs, 
            left_on='project_id', 
            right_on='project_id', 
            how='left'
        )
        projects_with_budget['allocated_cost'] = projects_with_budget['allocated_cost'].fillna(0.0)
        
        # Calculate remaining budget and utilization
        projects_with_budget['remaining_budget'] = projects_with_budget['max_budget'] - projects_with_budget['allocated_cost']
        projects_with_budget['budget_utilization_pct'] = (
            (projects_with_budget['allocated_cost'] / projects_with_budget['max_budget'] * 100)
            .round(2)
        ).fillna(0.0)
        
        # Add explanation of variance
        projects_with_budget['explanation_of_variance'] = project_explanations
        
        # Reorder columns to put budget info near max_budget
        cols = list(projects_with_budget.columns)
        # Move budget columns after max_budget
        budget_cols = ['allocated_cost', 'remaining_budget', 'budget_utilization_pct', 'explanation_of_variance']
        other_cols = [c for c in cols if c not in budget_cols]
        max_budget_idx = other_cols.index('max_budget')
        new_cols = other_cols[:max_budget_idx+1] + budget_cols + other_cols[max_budget_idx+1:]
        projects_with_budget = projects_with_budget[new_cols]
    else:
        # No allocations, set defaults
        projects_with_budget['allocated_cost'] = 0.0
        projects_with_budget['remaining_budget'] = projects_with_budget['max_budget']
        projects_with_budget['budget_utilization_pct'] = 0.0
        projects_with_budget['explanation_of_variance'] = "No allocations made - check solver status and constraints"
    
    # Add allocation summary to Employees sheet
    employees_with_allocation = employees.copy()
    if len(actual_allocations) > 0:
        # Calculate total allocated cost per employee (sum across all project allocations only)
        employee_summary = actual_allocations.groupby('employee_id').agg({
            'cost': 'sum',
            'allocation_fraction': 'sum'
        }).reset_index()
        employee_summary.columns = ['employee_id', 'total_allocated_cost', 'total_fte_months']
        
        # Calculate average FTE per month (total utilization including available capacity)
        # Use ALL allocations to get true monthly totals per employee
        # This represents the total FTE utilization per month (projects + available capacity = total capacity used)
        monthly_totals_all = all_allocations_for_util.groupby(['employee_id', 'month'])['allocation_fraction'].sum().reset_index()
        monthly_totals_all.columns = ['employee_id', 'month', 'monthly_fte_total']
        
        # Calculate average FTE per month per employee (mean of monthly totals)
        # This is the average total FTE utilization per month
        employee_monthly_avg = monthly_totals_all.groupby('employee_id').agg({
            'monthly_fte_total': 'mean',  # Average of monthly totals (avg total FTE per month)
            'month': 'nunique'  # Number of unique months
        }).reset_index()
        employee_monthly_avg.columns = ['employee_id', 'avg_fte_per_month', 'unique_months']
        employee_monthly_avg['avg_fte_per_month'] = employee_monthly_avg['avg_fte_per_month'].round(4)
        
        # Merge with employee_summary
        employee_summary = employee_summary.merge(
            employee_monthly_avg[['employee_id', 'avg_fte_per_month', 'unique_months']],
            on='employee_id',
            how='left'
        )
        employee_summary['total_allocated_fte'] = employee_summary['avg_fte_per_month']  # Average FTE per month
        employee_summary = employee_summary.drop(columns=['total_fte_months'])
        
        # Calculate yearly totals to verify constraint compliance
        actual_allocations['year'] = actual_allocations['month'].str[:4]
        yearly_summary = actual_allocations.groupby(['employee_id', 'year'])['allocation_fraction'].sum().reset_index()
        yearly_summary.columns = ['employee_id', 'year', 'yearly_fte']
        
        # Merge with employees
        employees_with_allocation = employees_with_allocation.merge(
            employee_summary[['employee_id', 'total_allocated_cost', 'total_allocated_fte', 'unique_months']],
            left_on='employee_id',
            right_on='employee_id',
            how='left'
        )
        employees_with_allocation['total_allocated_cost'] = employees_with_allocation['total_allocated_cost'].fillna(0.0)
        employees_with_allocation['total_allocated_fte'] = employees_with_allocation['total_allocated_fte'].fillna(0.0)
        employees_with_allocation['unique_months'] = employees_with_allocation['unique_months'].fillna(0).astype(int)
        
        # Add yearly breakdown as a string for reference
        yearly_breakdown = []
        for _, emp in employees_with_allocation.iterrows():
            eid = emp['employee_id']
            emp_yearly = yearly_summary[yearly_summary['employee_id'] == eid]
            if len(emp_yearly) > 0:
                breakdown_str = ', '.join([f"{row['year']}: {row['yearly_fte']:.2f}" for _, row in emp_yearly.iterrows()])
                yearly_breakdown.append(breakdown_str)
            else:
                yearly_breakdown.append('')
        employees_with_allocation['yearly_fte_breakdown'] = yearly_breakdown
        
        # Calculate utilization percentage (average FTE per month / capacity)
        employees_with_allocation['fte_utilization_pct'] = (
            (employees_with_allocation['total_allocated_fte'] / employees_with_allocation['fte_capacity'] * 100)
            .round(2)
        ).fillna(0.0)
        
        # Calculate remaining capacity
        employees_with_allocation['remaining_fte_capacity'] = (
            employees_with_allocation['fte_capacity'] - employees_with_allocation['total_allocated_fte']
        ).round(4)
        
        # Add explanation of variance
        employees_with_allocation['explanation_of_variance'] = employee_explanations
        
        # Reorder columns to put allocation info near cost_per_month
        cols = list(employees_with_allocation.columns)
        alloc_cols = ['total_allocated_cost', 'total_allocated_fte', 'unique_months', 'yearly_fte_breakdown', 'fte_utilization_pct', 'remaining_fte_capacity', 'explanation_of_variance']
        other_cols = [c for c in cols if c not in alloc_cols]
        cost_idx = other_cols.index('cost_per_month')
        new_cols = other_cols[:cost_idx+1] + alloc_cols + other_cols[cost_idx+1:]
        employees_with_allocation = employees_with_allocation[new_cols]
    else:
        # No allocations, set defaults
        employees_with_allocation['total_allocated_cost'] = 0.0
        employees_with_allocation['total_allocated_fte'] = 0.0
        employees_with_allocation['fte_utilization_pct'] = 0.0
        employees_with_allocation['remaining_fte_capacity'] = employees_with_allocation['fte_capacity']
        employees_with_allocation['explanation_of_variance'] = "No allocations made - check solver status and constraints"
    
    # Write reference data with budget/utilization info
    employees_with_allocation.to_excel(writer, sheet_name='Employees', index=False)
    projects_with_budget.to_excel(writer, sheet_name='Projects', index=False)

print(f'\n✓ Excel output written to {out_path}')

# Show detailed allocation breakdown
print('\n' + '=' * 80)
print('DETAILED ALLOCATION BREAKDOWN')
print('=' * 80)
if len(actual_allocations) > 0:
    print('\nBy Project:')
    for proj_id in actual_allocations['project_id'].unique():
        proj_allocs = actual_allocations[actual_allocations['project_id'] == proj_id]
        proj_name = proj_allocs.iloc[0]['project_name']
        print(f'\n  {proj_name} (ID: {proj_id}):')
        print(f'    Total FTE: {proj_allocs["allocation_fraction"].sum():.2f}')
        print(f'    Total Cost: ${proj_allocs["cost"].sum():,.2f}')
        print(f'    Allocations:')
        for _, alloc in proj_allocs.iterrows():
            print(f'      - {alloc["employee_name"]} ({alloc["employee_role"]}): '
                  f'{alloc["allocation_fraction"]:.2f} FTE in {alloc["month"]} = ${alloc["cost"]:,.2f}')
    
    print('\nBy Employee:')
    for emp_id in actual_allocations['employee_id'].unique():
        emp_allocs = actual_allocations[actual_allocations['employee_id'] == emp_id]
        emp_name = emp_allocs.iloc[0]['employee_name']
        print(f'\n  {emp_name} (ID: {emp_id}):')
        print(f'    Total FTE: {emp_allocs["allocation_fraction"].sum():.2f}')
        print(f'    Total Cost: ${emp_allocs["cost"].sum():,.2f}')
        print(f'    Projects: {", ".join(emp_allocs["project_name"].unique())}')

print('\n' + '=' * 80)
print('Test complete!')
print('=' * 80)
