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

# Write output Excel
with pd.ExcelWriter(str(out_path), engine='openpyxl') as writer:
    allocs_df.to_excel(writer, sheet_name='Allocations', index=False)
    if len(actual_allocations) > 0:
        actual_allocations.to_excel(writer, sheet_name='Project_Allocations', index=False)
    if len(available_capacity) > 0:
        available_capacity.to_excel(writer, sheet_name='Available_Capacity', index=False)
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
