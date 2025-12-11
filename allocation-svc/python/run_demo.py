"""run_demo.py - demo that runs allocator with local sample Excel template
This script uses the excel template to load data, run allocator, and write allocations back to Excel and/or database.
"""
import pandas as pd, json
import os
from pathlib import Path
from allocate_fully_optimized import fully_optimized_allocator
from excel_io import create_template
from db import connect, write_allocations, load_table
from scenario import create_scenario, record_history


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

allocs = fully_optimized_allocator(
    employees, projects, scenario_id,
    global_start=global_start, global_end=global_end
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
    
    # Reference data
    employees.to_excel(writer, sheet_name='Employees', index=False)
    projects.to_excel(writer, sheet_name='Projects', index=False)

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
