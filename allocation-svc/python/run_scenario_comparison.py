"""run_scenario_comparison.py - Compare allocations across multiple scenarios
Creates scenario 2 with different parameters and compares with scenario 1
Optionally saves to database if SAVE_TO_DB=true
"""
import pandas as pd
import json
import os
from pathlib import Path
from allocate_fully_optimized import fully_optimized_allocator
from excel_io import create_template
from db import connect, write_allocations, load_table
from scenario import create_scenario, record_history

BASE = Path(__file__).resolve().parent
excel_path = BASE.parent / 'excel' / 'budget_planner_template.xlsx'
out_path = BASE.parent / 'excel' / 'scenario_comparison.xlsx'

# Create template if not exists
if not excel_path.exists():
    create_template(str(excel_path))

# Load data
xls = pd.ExcelFile(str(excel_path))
employees = pd.read_excel(xls, 'Employees')
projects = pd.read_excel(xls, 'Projects')
scenarios = pd.read_excel(xls, 'Scenarios')

global_start = projects['start_month'].min()
global_end = projects['end_month'].max()

print("=" * 80)
print("SCENARIO COMPARISON")
print("=" * 80)

# Scenario 1: Baseline (current allocations)
print("\n1. Running Scenario 1 (Baseline)...")
scenario_id_1 = 1
allocs_1 = fully_optimized_allocator(
    employees, projects, scenario_id_1,
    global_start=global_start, global_end=global_end,
    weights={
        'cost_weight': 0.5,
        'skill_weight': 0.2,
        'fragmentation_weight': 0.1,
        'continuity_weight': 0.1,
        'balance_weight': 0.1,
        'preference_weight': 0.0,
        'diversity_weight': 0.0,
        'leveling_weight': 0.0
    },
    config={
        'max_employee_per_project': 1.0,
        'min_team_size': 1,
        'allow_skill_development': False,
        'skill_dev_max_fte': 0.2,
        'discrete_allocations': False,
        'allocation_increments': [0.25, 0.5, 0.75, 1.0],
        'budget_flexibility': False,
        'max_budget_borrow': 1,
        'enable_team_diversity': False,
        'min_grade_diversity': False,
        'enable_employee_preferences': False
    }
)
allocs_1_df = pd.DataFrame(allocs_1)

if len(allocs_1) > 0:
    actual_1 = allocs_1_df[allocs_1_df['project_id'].notna()]
    print(f"   Allocations: {len(actual_1)}")
    print(f"   Total cost: ${actual_1['cost'].sum():,.2f}")
    print(f"   Total FTE allocated: {actual_1['allocation_fraction'].sum():.2f}")

# Scenario 2: Modified scenario with different parameters
print("\n2. Running Scenario 2 (Modified - Higher Budget & Different Priorities)...")

# Create modified projects for scenario 2
projects_2 = projects.copy()
# Increase budgets significantly to allow more allocation
projects_2['max_budget'] = projects_2['max_budget'] * 2.0  # 100% budget increase

# Also modify project Alpha to have higher priority (by increasing budget more)
alpha_idx = projects_2[projects_2['project_name'] == 'Alpha'].index
if len(alpha_idx) > 0:
    projects_2.loc[alpha_idx, 'max_budget'] = projects_2.loc[alpha_idx, 'max_budget'] * 1.5

print(f"   Project budgets:")
for _, proj in projects_2.iterrows():
    orig_budget = projects[projects['project_id'] == proj['project_id']]['max_budget'].iloc[0]
    print(f"     {proj['project_name']}: ${orig_budget:,.0f} -> ${proj['max_budget']:,.0f}")

scenario_id_2 = 2
# Use fully optimized allocator with different weights for scenario 2
print("   Using fully optimized allocator with different optimization weights...")
allocs_2 = fully_optimized_allocator(
    employees, projects_2, scenario_id_2,
    global_start=global_start, global_end=global_end,
    weights={
        'cost_weight': 0.3,  # Lower cost weight
        'skill_weight': 0.3,  # Higher skill weight
        'fragmentation_weight': 0.2,  # Higher fragmentation penalty
        'continuity_weight': 0.1,
        'balance_weight': 0.05,
        'preference_weight': 0.03,
        'diversity_weight': 0.01,
        'leveling_weight': 0.01
    },
    config={
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
        'enable_employee_preferences': True
    }
)
allocs_2_df = pd.DataFrame(allocs_2)

if len(allocs_2) > 0:
    actual_2 = allocs_2_df[allocs_2_df['project_id'].notna()]
    print(f"   Allocations: {len(actual_2)}")
    print(f"   Total cost: ${actual_2['cost'].sum():,.2f}")
    print(f"   Total FTE allocated: {actual_2['allocation_fraction'].sum():.2f}")

# Comparison Analysis
print("\n" + "=" * 80)
print("COMPARISON ANALYSIS")
print("=" * 80)

if len(allocs_1) > 0 and len(allocs_2) > 0:
    actual_1 = allocs_1_df[allocs_1_df['project_id'].notna()].copy()
    actual_2 = allocs_2_df[allocs_2_df['project_id'].notna()].copy()
    
    # Summary comparison
    summary_data = []
    summary_data.append({
        'Metric': 'Total Allocations',
        'Scenario 1': len(actual_1),
        'Scenario 2': len(actual_2),
        'Delta': len(actual_2) - len(actual_1),
        'Change %': f"{((len(actual_2) - len(actual_1)) / len(actual_1) * 100):.1f}%"
    })
    
    cost_1 = actual_1['cost'].sum()
    cost_2 = actual_2['cost'].sum()
    summary_data.append({
        'Metric': 'Total Cost',
        'Scenario 1': f"${cost_1:,.2f}",
        'Scenario 2': f"${cost_2:,.2f}",
        'Delta': f"${cost_2 - cost_1:,.2f}",
        'Change %': f"{((cost_2 - cost_1) / cost_1 * 100):.1f}%"
    })
    
    fte_1 = actual_1['allocation_fraction'].sum()
    fte_2 = actual_2['allocation_fraction'].sum()
    summary_data.append({
        'Metric': 'Total FTE Allocated',
        'Scenario 1': f"{fte_1:.2f}",
        'Scenario 2': f"{fte_2:.2f}",
        'Delta': f"{fte_2 - fte_1:.2f}",
        'Change %': f"{((fte_2 - fte_1) / fte_1 * 100):.1f}%"
    })
    
    summary_df = pd.DataFrame(summary_data)
    print("\nSummary Comparison:")
    print(summary_df.to_string(index=False))
    
    # By Project comparison
    print("\n\nBy Project Comparison:")
    proj_1 = actual_1.groupby('project_name').agg({
        'cost': 'sum',
        'allocation_fraction': 'sum'
    }).round(2)
    proj_1.columns = ['Cost_S1', 'FTE_S1']
    
    proj_2 = actual_2.groupby('project_name').agg({
        'cost': 'sum',
        'allocation_fraction': 'sum'
    }).round(2)
    proj_2.columns = ['Cost_S2', 'FTE_S2']
    
    proj_comp = pd.concat([proj_1, proj_2], axis=1)
    proj_comp['Cost_Delta'] = proj_comp['Cost_S2'] - proj_comp['Cost_S1']
    proj_comp['FTE_Delta'] = proj_comp['FTE_S2'] - proj_comp['FTE_S1']
    print(proj_comp.to_string())
    
    # By Employee comparison
    print("\n\nBy Employee Comparison:")
    emp_1 = actual_1.groupby('employee_name').agg({
        'cost': 'sum',
        'allocation_fraction': 'sum'
    }).round(2)
    emp_1.columns = ['Cost_S1', 'FTE_S1']
    
    emp_2 = actual_2.groupby('employee_name').agg({
        'cost': 'sum',
        'allocation_fraction': 'sum'
    }).round(2)
    emp_2.columns = ['Cost_S2', 'FTE_S2']
    
    emp_comp = pd.concat([emp_1, emp_2], axis=1)
    emp_comp['Cost_Delta'] = emp_comp['Cost_S2'] - emp_comp['Cost_S1']
    emp_comp['FTE_Delta'] = emp_comp['FTE_S2'] - emp_comp['FTE_S1']
    print(emp_comp.to_string())
    
    # By Month comparison
    print("\n\nBy Month Comparison:")
    month_1 = actual_1.groupby('month').agg({
        'cost': 'sum',
        'allocation_fraction': 'sum'
    }).round(2)
    month_1.columns = ['Cost_S1', 'FTE_S1']
    
    month_2 = actual_2.groupby('month').agg({
        'cost': 'sum',
        'allocation_fraction': 'sum'
    }).round(2)
    month_2.columns = ['Cost_S2', 'FTE_S2']
    
    month_comp = pd.concat([month_1, month_2], axis=1)
    month_comp['Cost_Delta'] = month_comp['Cost_S2'] - month_comp['Cost_S1']
    month_comp['FTE_Delta'] = month_comp['FTE_S2'] - month_comp['FTE_S1']
    print(month_comp.to_string())
    
    # Detailed comparison - show all allocations side by side
    print("\n\nDetailed Allocation Comparison:")
    
    # Create full comparison by employee-project-month
    all_keys = set()
    for _, row in actual_1.iterrows():
        key = (row['employee_name'], row['project_name'], row['month'])
        all_keys.add(key)
    for _, row in actual_2.iterrows():
        key = (row['employee_name'], row['project_name'], row['month'])
        all_keys.add(key)
    
    comparison_rows = []
    for emp_name, proj_name, month in sorted(all_keys):
        s1_row = actual_1[(actual_1['employee_name'] == emp_name) & 
                         (actual_1['project_name'] == proj_name) & 
                         (actual_1['month'] == month)]
        s2_row = actual_2[(actual_2['employee_name'] == emp_name) & 
                         (actual_2['project_name'] == proj_name) & 
                         (actual_2['month'] == month)]
        
        fte_s1 = s1_row['allocation_fraction'].sum() if len(s1_row) > 0 else 0.0
        cost_s1 = s1_row['cost'].sum() if len(s1_row) > 0 else 0.0
        fte_s2 = s2_row['allocation_fraction'].sum() if len(s2_row) > 0 else 0.0
        cost_s2 = s2_row['cost'].sum() if len(s2_row) > 0 else 0.0
        
        if abs(fte_s1 - fte_s2) > 0.001 or abs(cost_s1 - cost_s2) > 0.01:
            comparison_rows.append({
                'employee_name': emp_name,
                'project_name': proj_name,
                'month': month,
                'FTE_S1': round(fte_s1, 4),
                'FTE_S2': round(fte_s2, 4),
                'FTE_Delta': round(fte_s2 - fte_s1, 4),
                'Cost_S1': round(cost_s1, 2),
                'Cost_S2': round(cost_s2, 2),
                'Cost_Delta': round(cost_s2 - cost_s1, 2)
            })
    
    if len(comparison_rows) > 0:
        changed_df = pd.DataFrame(comparison_rows)
        print("\nChanged Allocations:")
        print(changed_df.to_string(index=False))
    else:
        print("No changes in allocations between scenarios")
        changed_df = pd.DataFrame()
    
    # Write to Excel
    print("\n" + "=" * 80)
    print("Writing comparison to Excel...")
    
    with pd.ExcelWriter(str(out_path), engine='openpyxl') as writer:
        # Summary
        summary_df.to_excel(writer, sheet_name='Summary', index=False)
        
        # Scenario 1 allocations
        allocs_1_df.to_excel(writer, sheet_name='Scenario_1_All', index=False)
        actual_1.to_excel(writer, sheet_name='Scenario_1_Projects', index=False)
        avail_1 = allocs_1_df[(allocs_1_df['available_capacity'] == True) | 
                              (allocs_1_df['project_id'].isna())]
        if len(avail_1) > 0:
            avail_1.to_excel(writer, sheet_name='Scenario_1_Available', index=False)
        
        # Scenario 2 allocations
        allocs_2_df.to_excel(writer, sheet_name='Scenario_2_All', index=False)
        actual_2.to_excel(writer, sheet_name='Scenario_2_Projects', index=False)
        avail_2 = allocs_2_df[(allocs_2_df['available_capacity'] == True) | 
                              (allocs_2_df['project_id'].isna())]
        if len(avail_2) > 0:
            avail_2.to_excel(writer, sheet_name='Scenario_2_Available', index=False)
        
        # Comparisons
        proj_comp.to_excel(writer, sheet_name='Comparison_By_Project', index=True)
        emp_comp.to_excel(writer, sheet_name='Comparison_By_Employee', index=True)
        month_comp.to_excel(writer, sheet_name='Comparison_By_Month', index=True)
        
        if len(changed_df) > 0:
            changed_df.to_excel(writer, sheet_name='Changed_Allocations', index=False)
        
        # Side-by-side comparison
        all_comparison = []
        for emp_name, proj_name, month in sorted(all_keys):
            s1_row = actual_1[(actual_1['employee_name'] == emp_name) & 
                             (actual_1['project_name'] == proj_name) & 
                             (actual_1['month'] == month)]
            s2_row = actual_2[(actual_2['employee_name'] == emp_name) & 
                             (actual_2['project_name'] == proj_name) & 
                             (actual_2['month'] == month)]
            
            fte_s1 = s1_row['allocation_fraction'].sum() if len(s1_row) > 0 else 0.0
            cost_s1 = s1_row['cost'].sum() if len(s1_row) > 0 else 0.0
            fte_s2 = s2_row['allocation_fraction'].sum() if len(s2_row) > 0 else 0.0
            cost_s2 = s2_row['cost'].sum() if len(s2_row) > 0 else 0.0
            
            all_comparison.append({
                'employee_name': emp_name,
                'project_name': proj_name,
                'month': month,
                'FTE_S1': round(fte_s1, 4),
                'FTE_S2': round(fte_s2, 4),
                'FTE_Delta': round(fte_s2 - fte_s1, 4),
                'Cost_S1': round(cost_s1, 2),
                'Cost_S2': round(cost_s2, 2),
                'Cost_Delta': round(cost_s2 - cost_s1, 2)
            })
        
        if len(all_comparison) > 0:
            all_comp_df = pd.DataFrame(all_comparison)
            all_comp_df.to_excel(writer, sheet_name='All_Allocations_Comparison', index=False)
        
        # Original data
        employees.to_excel(writer, sheet_name='Employees', index=False)
        projects.to_excel(writer, sheet_name='Projects_Original', index=False)
        projects_2.to_excel(writer, sheet_name='Projects_Scenario2', index=False)
    
    print(f"✓ Comparison written to: {out_path}")
    
    # Optionally save to database
    save_to_db = os.getenv('SAVE_TO_DB', 'false').lower() == 'true'
    if save_to_db:
        try:
            print("\nSaving to database...")
            db_server = os.getenv('DB_SERVER', 'localhost\\SQLEXPRESS')
            db_name = os.getenv('DB_NAME', 'budgetdb')
            db_user = os.getenv('DB_USER', None)
            db_password = os.getenv('DB_PASSWORD', None)
            created_by = os.getenv('CREATED_BY', 'system')
            use_trusted = db_user is None or db_user == ''
            
            conn = connect(server=db_server, database=db_name, trusted=use_trusted, user=db_user, password=db_password)
            print(f'  Connected to {db_server}/{db_name}')
            
            # Save Scenario 1
            if len(actual_1) > 0:
                s1_name = f"Scenario {scenario_id_1} - Baseline"
                try:
                    existing = load_table(conn, 'scenario')
                    if scenario_id_1 not in existing['scenario_id'].values:
                        create_scenario(conn, s1_name, created_by, None)
                except:
                    create_scenario(conn, s1_name, created_by, None)
                
                db_alloc_1 = actual_1[actual_1['project_id'].notna()].copy()
                if 'scenario_id' not in db_alloc_1.columns:
                    db_alloc_1['scenario_id'] = scenario_id_1
                write_allocations(conn, db_alloc_1)
                print(f'  ✓ Saved Scenario 1: {len(db_alloc_1)} allocations')
            
            # Save Scenario 2
            if len(actual_2) > 0:
                s2_name = f"Scenario {scenario_id_2} - Modified"
                try:
                    existing = load_table(conn, 'scenario')
                    if scenario_id_2 not in existing['scenario_id'].values:
                        create_scenario(conn, s2_name, created_by, scenario_id_1)
                except:
                    create_scenario(conn, s2_name, created_by, scenario_id_1)
                
                db_alloc_2 = actual_2[actual_2['project_id'].notna()].copy()
                if 'scenario_id' not in db_alloc_2.columns:
                    db_alloc_2['scenario_id'] = scenario_id_2
                write_allocations(conn, db_alloc_2)
                print(f'  ✓ Saved Scenario 2: {len(db_alloc_2)} allocations')
            
            conn.close()
            print('  ✓ Database save complete')
        except Exception as e:
            print(f'  ⚠ Could not save to database: {e}')
    else:
        print("\nNote: Database saving is disabled. Set SAVE_TO_DB=true to enable.")
    
else:
    print("Error: Could not generate allocations for comparison")

print("\n" + "=" * 80)
print("Comparison complete!")
print("=" * 80)

