"""run_demo_with_db.py - Demo with database integration
Saves allocations to both Excel and database
"""
import pandas as pd
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

# Database configuration (from environment or defaults)
DB_SERVER = os.getenv('DB_SERVER', 'localhost\\SQLEXPRESS')
DB_NAME = os.getenv('DB_NAME', 'budgetdb')
DB_USER = os.getenv('DB_USER', None)
DB_PASSWORD = os.getenv('DB_PASSWORD', None)
CREATED_BY = os.getenv('CREATED_BY', 'system')

# Create template if not exists
if not excel_path.exists():
    create_template(str(excel_path))

# Load data
xls = pd.ExcelFile(str(excel_path))
employees = pd.read_excel(xls, 'Employees')
projects = pd.read_excel(xls, 'Projects')
scenarios = pd.read_excel(xls, 'Scenarios')

# Choose scenario id
scenario_id = 1
global_start = projects['start_month'].min()
global_end = projects['end_month'].max()

print("=" * 80)
print("ALLOCATION DEMO WITH DATABASE INTEGRATION")
print("=" * 80)

# Run allocator
print("\nRunning allocator...")
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

# Create pivot views
print("\nCreating pivot views...")
monthly_pivot, quarterly_pivot = create_pivot_views(allocs_df)
print(f'  Monthly pivot: {len(monthly_pivot)} rows')
print(f'  Quarterly pivot: {len(quarterly_pivot)} rows')

# Write to Excel
print("\nWriting to Excel...")
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
    
    # Reference data
    employees.to_excel(writer, sheet_name='Employees', index=False)
    projects.to_excel(writer, sheet_name='Projects', index=False)
print(f'✓ Excel output written to {out_path}')

# Save to database
print("\nSaving to database...")
try:
    use_trusted = DB_USER is None or DB_USER == ''
    conn = connect(
        server=DB_SERVER,
        database=DB_NAME,
        trusted=use_trusted,
        user=DB_USER,
        password=DB_PASSWORD
    )
    print(f'  Connected to {DB_SERVER}/{DB_NAME}')
    
    # Create or verify scenario
    scenario_name = f"Scenario {scenario_id}"
    try:
        existing_scenarios = load_table(conn, 'scenario')
        if scenario_id not in existing_scenarios['scenario_id'].values:
            new_scenario_id = create_scenario(conn, scenario_name, CREATED_BY)
            print(f'  ✓ Created scenario: {new_scenario_id} - {scenario_name}')
        else:
            print(f'  ✓ Using existing scenario: {scenario_id} - {scenario_name}')
    except Exception as e:
        print(f'  Note: {e}')
        # Try to create scenario
        try:
            new_scenario_id = create_scenario(conn, scenario_name, CREATED_BY)
            print(f'  ✓ Created scenario: {new_scenario_id} - {scenario_name}')
        except Exception as e2:
            print(f'  ⚠ Could not create scenario: {e2}')
    
    # Save allocations
    if len(actual_allocations) > 0:
        db_allocations = actual_allocations[actual_allocations['project_id'].notna()].copy()
        if 'scenario_id' not in db_allocations.columns:
            db_allocations['scenario_id'] = scenario_id
        
        write_allocations(conn, db_allocations)
        print(f'  ✓ Saved {len(db_allocations)} allocations to database')
        
        # Record history
        record_history(
            conn, scenario_id, 'allocation', None,
            None, {
                'count': len(db_allocations), 
                'total_cost': float(db_allocations['cost'].sum()),
                'total_fte': float(db_allocations['allocation_fraction'].sum())
            },
            CREATED_BY
        )
        print(f'  ✓ Recorded history log')
    
    conn.close()
    print('✓ Database save complete')
    
except Exception as e:
    print(f'  ⚠ Error saving to database: {e}')
    print('  Make sure:')
    print('    1. SQL Server is running')
    print('    2. Database "budgetdb" exists')
    print('    3. Schema is created (run schema.sql)')
    print('    4. Connection settings are correct')

print("\n" + "=" * 80)
print("Demo complete!")
print("=" * 80)

