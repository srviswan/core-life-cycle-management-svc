"""run_demo.py - demo that runs allocator with local sample Excel template
This script uses the excel template to load data, run allocator, and write allocations back to Excel and/or database.
"""
import pandas as pd, json
import os
import sys
from pathlib import Path
from allocate_fully_optimized import fully_optimized_allocator
from excel_io import create_template
from db import connect, write_allocations, load_table
from scenario import create_scenario, record_history
from config_loader import get_config


def generate_variance_explanations(projects, employees, actual_allocations, config=None):
    """Generate explanations for why projects and employees are not fully allocated."""
    if config is None:
        config = {}
    
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
            try:
                months = pd.date_range(
                    start=proj['start_month'] + "-01", 
                    end=proj['end_month'] + "-01", 
                    freq='MS'
                ).strftime('%Y-%m').tolist()
                num_months = len(months)
            except:
                num_months = 1
            
            max_possible_monthly = total_employee_cost / num_months if num_months > 0 else total_employee_cost
            per_month_budget = max_budget / num_months if num_months > 0 else max_budget
            
            if per_month_budget > max_possible_monthly * 1.2:  # 20% buffer
                reasons.append(f"Budget too large: ${per_month_budget:,.0f}/month exceeds max possible ${max_possible_monthly:,.0f}/month")
            
            # Reason 2: Not enough employees with matching skills
            req_skills = {}
            try:
                req_skills = json.loads(proj.get('required_skills', '{}'))
            except:
                pass
            
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
                req_skills = {}
                try:
                    req_skills = json.loads(proj.get('required_skills', '{}'))
                except:
                    pass
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


def create_pivot_views(allocations_df):
    """Create pivot table views with employee+project as rows and months/quarters as columns.
    
    Returns:
        tuple: (monthly_pivot, quarterly_pivot)
    """
    # Filter to actual allocations only
    df = allocations_df[allocations_df['project_id'].notna()].copy()
    
    if len(df) == 0:
        return pd.DataFrame(), pd.DataFrame()
    
    # Create employee-project key
    df['employee_project'] = df['employee_name'] + ' - ' + df['project_name']
    
    # Ensure month is string format
    df['month'] = df['month'].astype(str)
    
    # Create monthly pivot: employee+project rows, months as columns
    monthly_pivot = df.pivot_table(
        index=['employee_id', 'employee_name', 'employee_role', 'project_id', 'project_name', 'employee_project'],
        columns='month',
        values='allocation_fraction',
        aggfunc='sum',
        fill_value=0.0
    ).reset_index()
    
    # Rename columns to remove 'month' from column names
    monthly_pivot.columns.name = None
    
    # Create quarter mapping
    def get_quarter(month_str):
        """Convert YYYY-MM to quarter string (e.g., '2025-01' -> '2025-Q1')"""
        try:
            year, month = month_str.split('-')
            quarter = (int(month) - 1) // 3 + 1
            return f"{year}-Q{quarter}"
        except:
            return month_str
    
    # Add quarter column
    df['quarter'] = df['month'].apply(get_quarter)
    
    # Create quarterly pivot
    quarterly_pivot = df.pivot_table(
        index=['employee_id', 'employee_name', 'employee_role', 'project_id', 'project_name', 'employee_project'],
        columns='quarter',
        values='allocation_fraction',
        aggfunc='sum',
        fill_value=0.0
    ).reset_index()
    
    # Rename columns to remove 'quarter' from column names
    quarterly_pivot.columns.name = None
    
    # Sort columns: first the index columns, then months/quarters in order
    def sort_columns(df_pivot, date_cols):
        """Sort date columns chronologically"""
        index_cols = [col for col in df_pivot.columns if col not in date_cols]
        sorted_date_cols = sorted(date_cols)
        return df_pivot[index_cols + sorted_date_cols]
    
    # Get date columns for monthly pivot
    monthly_date_cols = [col for col in monthly_pivot.columns if col not in 
                        ['employee_id', 'employee_name', 'employee_role', 'project_id', 'project_name', 'employee_project']]
    monthly_pivot = sort_columns(monthly_pivot, monthly_date_cols)
    
    # Get date columns for quarterly pivot
    quarterly_date_cols = [col for col in quarterly_pivot.columns if col not in 
                          ['employee_id', 'employee_name', 'employee_role', 'project_id', 'project_name', 'employee_project']]
    quarterly_pivot = sort_columns(quarterly_pivot, quarterly_date_cols)
    
    return monthly_pivot, quarterly_pivot

BASE = Path(__file__).resolve().parent
excel_path = BASE.parent / 'excel' / 'budget_planner_template.xlsx'
out_path = BASE.parent / 'excel' / 'budget_planner_allocations.xlsx'

# create template if not exists
if not excel_path.exists():
    create_template(str(excel_path))

# load
xls = pd.ExcelFile(str(excel_path))
employees = pd.read_excel(xls, 'Employees')
projects = pd.read_excel(xls, 'Projects')
scenarios = pd.read_excel(xls, 'Scenarios')

# choose scenario id 1
scenario_id = 1
# run allocator for all projects between min(start_month) and max(end_month)
global_start = projects['start_month'].min()
global_end = projects['end_month'].max()

# Load configuration from file or use defaults
# Usage: python run_demo.py [config_file_path]
config_path = sys.argv[1] if len(sys.argv) > 1 else None
try:
    config = get_config(config_path, validate=True)
    if config:
        print(f"✓ Loaded configuration from file")
except Exception as e:
    print(f"⚠ Warning: Could not load config file: {e}")
    print("   Using default configuration (empty config)")
    config = {}  # Empty config uses defaults

allocs = fully_optimized_allocator(
    employees, projects, scenario_id,
    global_start=global_start, global_end=global_end,
    config=config
)
allocs_df = pd.DataFrame(allocs)

# Separate actual allocations from available capacity
actual_allocations = allocs_df[allocs_df['project_id'].notna()].copy()
available_capacity = allocs_df[(allocs_df['available_capacity'] == True) | (allocs_df['project_id'].isna())].copy()

print(f'\nAllocation Summary:')
print(f'  Actual allocations: {len(actual_allocations)}')
print(f'  Total cost: ${actual_allocations["cost"].sum():,.2f}')
print(f'  Available capacity records: {len(available_capacity)}')
if len(available_capacity) > 0:
    print(f'  Total available FTE: {available_capacity["allocation_fraction"].sum():.2f}')

# Check for allocations without required skills
no_skills_allocations = pd.DataFrame()
if 'no_required_skills' in actual_allocations.columns:
    no_skills_allocations = actual_allocations[actual_allocations['no_required_skills'] == True].copy()
    if len(no_skills_allocations) > 0:
        print(f'\n⚠️  Warning: {len(no_skills_allocations)} allocation(s) made without required skills')
        print(f'  Total cost of no-skills allocations: ${no_skills_allocations["cost"].sum():,.2f}')
        print(f'  Consider reviewing these allocations or adding employees with matching skills')

# Check for skill development allocations
skill_dev_allocations = pd.DataFrame()
if 'skill_development' in actual_allocations.columns:
    skill_dev_allocations = actual_allocations[actual_allocations['skill_development'] == True].copy()
    if len(skill_dev_allocations) > 0:
        print(f'\n📚 Skill Development: {len(skill_dev_allocations)} allocation(s) for skill development')
        print(f'  Total FTE: {skill_dev_allocations["allocation_fraction"].sum():.2f}')

# Generate variance explanations
print('\nGenerating variance explanations...')
project_explanations, employee_explanations = generate_variance_explanations(
    projects, employees, actual_allocations, config
)

# Create pivot views
print('\nCreating pivot views...')
monthly_pivot, quarterly_pivot = create_pivot_views(allocs_df)
print(f'  Monthly pivot: {len(monthly_pivot)} rows')
print(f'  Quarterly pivot: {len(quarterly_pivot)} rows')

# Write output Excel
with pd.ExcelWriter(str(out_path), engine='openpyxl') as writer:
    # Original format sheets
    allocs_df.to_excel(writer, sheet_name='Allocations', index=False)
    if len(actual_allocations) > 0:
        actual_allocations.to_excel(writer, sheet_name='Project_Allocations', index=False)
    if len(available_capacity) > 0:
        available_capacity.to_excel(writer, sheet_name='Available_Capacity', index=False)
    
    # New pivot format sheets
    if len(monthly_pivot) > 0:
        monthly_pivot.to_excel(writer, sheet_name='Monthly_View', index=False)
        print('  ✓ Created Monthly_View sheet (employee+project rows, month columns)')
    if len(quarterly_pivot) > 0:
        quarterly_pivot.to_excel(writer, sheet_name='Quarterly_View', index=False)
        print('  ✓ Created Quarterly_View sheet (employee+project rows, quarter columns)')
    
    # Skill gap report
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
        budget_cols = ['allocated_cost', 'remaining_budget', 'budget_utilization_pct', 'explanation_of_variance']
        other_cols = [c for c in cols if c not in budget_cols]
        if 'max_budget' in other_cols:
            max_budget_idx = other_cols.index('max_budget')
            new_cols = other_cols[:max_budget_idx+1] + budget_cols + other_cols[max_budget_idx+1:]
            projects_with_budget = projects_with_budget[new_cols]
    else:
        projects_with_budget['allocated_cost'] = 0.0
        projects_with_budget['remaining_budget'] = projects_with_budget['max_budget']
        projects_with_budget['budget_utilization_pct'] = 0.0
        projects_with_budget['explanation_of_variance'] = "No allocations made - check solver status and constraints"
    
    # Add allocation summary to Employees sheet
    employees_with_allocation = employees.copy()
    if len(actual_allocations) > 0:
        # Calculate total allocated cost and FTE per employee
        employee_summary = actual_allocations.groupby('employee_id').agg({
            'cost': 'sum',
            'allocation_fraction': 'sum'
        }).reset_index()
        employee_summary.columns = ['employee_id', 'total_allocated_cost', 'total_allocated_fte']
        
        # Merge with employees
        employees_with_allocation = employees_with_allocation.merge(
            employee_summary,
            left_on='employee_id',
            right_on='employee_id',
            how='left'
        )
        employees_with_allocation['total_allocated_cost'] = employees_with_allocation['total_allocated_cost'].fillna(0.0)
        employees_with_allocation['total_allocated_fte'] = employees_with_allocation['total_allocated_fte'].fillna(0.0)
        
        # Calculate utilization percentage
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
        
        # Reorder columns
        cols = list(employees_with_allocation.columns)
        alloc_cols = ['total_allocated_cost', 'total_allocated_fte', 'fte_utilization_pct', 'remaining_fte_capacity', 'explanation_of_variance']
        other_cols = [c for c in cols if c not in alloc_cols]
        if 'cost_per_month' in other_cols:
            cost_idx = other_cols.index('cost_per_month')
            new_cols = other_cols[:cost_idx+1] + alloc_cols + other_cols[cost_idx+1:]
            employees_with_allocation = employees_with_allocation[new_cols]
    else:
        employees_with_allocation['total_allocated_cost'] = 0.0
        employees_with_allocation['total_allocated_fte'] = 0.0
        employees_with_allocation['fte_utilization_pct'] = 0.0
        employees_with_allocation['remaining_fte_capacity'] = employees_with_allocation['fte_capacity']
        employees_with_allocation['explanation_of_variance'] = "No allocations made - check solver status and constraints"
    
    # Write reference data with budget/utilization info
    employees_with_allocation.to_excel(writer, sheet_name='Employees', index=False)
    projects_with_budget.to_excel(writer, sheet_name='Projects', index=False)

print('✓ Excel output written to', out_path)

# Optionally save to database
save_to_db = os.getenv('SAVE_TO_DB', 'false').lower() == 'true'
if save_to_db:
    try:
        print('\nSaving to database...')
        # Connect to database
        db_server = os.getenv('DB_SERVER', 'localhost\\SQLEXPRESS')
        db_name = os.getenv('DB_NAME', 'budgetdb')
        db_user = os.getenv('DB_USER', None)
        db_password = os.getenv('DB_PASSWORD', None)
        use_trusted = db_user is None or db_user == ''
        
        conn = connect(
            server=db_server,
            database=db_name,
            trusted=use_trusted,
            user=db_user,
            password=db_password
        )
        
        # Create scenario if it doesn't exist
        scenario_name = f"Scenario {scenario_id}"
        created_by = os.getenv('CREATED_BY', 'system')
        
        # Check if scenario exists
        try:
            existing_scenarios = load_table(conn, 'scenario')
            if scenario_id not in existing_scenarios['scenario_id'].values:
                new_scenario_id = create_scenario(conn, scenario_name, created_by)
                print(f'  Created scenario: {new_scenario_id} - {scenario_name}')
            else:
                print(f'  Using existing scenario: {scenario_id} - {scenario_name}')
        except Exception as e:
            print(f'  Note: Could not check existing scenarios: {e}')
            # Try to create scenario anyway
            try:
                new_scenario_id = create_scenario(conn, scenario_name, created_by)
                print(f'  Created scenario: {new_scenario_id} - {scenario_name}')
            except:
                pass
        
        # Save only actual allocations (not available capacity) to database
        if len(actual_allocations) > 0:
            # Filter out available capacity records
            db_allocations = actual_allocations[actual_allocations['project_id'].notna()].copy()
            
            # Ensure required columns exist
            if 'scenario_id' not in db_allocations.columns:
                db_allocations['scenario_id'] = scenario_id
            
            write_allocations(conn, db_allocations)
            print(f'  ✓ Saved {len(db_allocations)} allocations to database')
        
        # Record history
        if len(actual_allocations) > 0:
            record_history(
                conn, scenario_id, 'allocation', None,
                None, {'count': len(actual_allocations), 'total_cost': float(actual_allocations['cost'].sum())},
                created_by
            )
            print(f'  ✓ Recorded history log')
        
        conn.close()
        print('✓ Database save complete')
        
    except Exception as e:
        print(f'  ⚠ Warning: Could not save to database: {e}')
        print('  Set SAVE_TO_DB=true and configure database connection to enable DB saving')
else:
    print('\nNote: Database saving is disabled. Set SAVE_TO_DB=true to enable.')

print('\nDemo run complete!')
