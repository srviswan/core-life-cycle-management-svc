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

# Write to Excel
print("\nWriting to Excel...")
with pd.ExcelWriter(str(out_path), engine='openpyxl') as writer:
    allocs_df.to_excel(writer, sheet_name='Allocations', index=False)
    if len(actual_allocations) > 0:
        actual_allocations.to_excel(writer, sheet_name='Project_Allocations', index=False)
    if len(available_capacity) > 0:
        available_capacity.to_excel(writer, sheet_name='Available_Capacity', index=False)
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

