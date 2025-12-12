"""run_budget_allocator.py - Main execution script for budget allocator"""
import pandas as pd
import sys
from pathlib import Path
from datetime import datetime
from collections import defaultdict
from typing import List, Dict, Tuple
from budget_allocator import budget_allocator
from utils import (
    calculate_project_priority, generate_allocation_explanation,
    calculate_skill_match_score, check_mandatory_skills, parse_required_skills
)
from config import PRIORITY_WEIGHTS


def read_input_excel(input_path: str) -> Tuple[pd.DataFrame, pd.DataFrame]:
    """Read projects and resources from Excel file.
    
    Args:
        input_path: Path to input Excel file
    
    Returns:
        Tuple of (projects_df, resources_df)
    """
    xls = pd.ExcelFile(input_path)
    
    if 'Projects' not in xls.sheet_names:
        raise ValueError("Excel file must have 'Projects' sheet")
    if 'Resources' not in xls.sheet_names:
        raise ValueError("Excel file must have 'Resources' sheet")
    
    projects_df = pd.read_excel(xls, 'Projects')
    resources_df = pd.read_excel(xls, 'Resources')
    
    return projects_df, resources_df


def generate_output_excel(allocations: List[Dict], projects_df: pd.DataFrame,
                         resources_df: pd.DataFrame, output_path: str):
    """Generate output Excel with multiple sheets.
    
    Args:
        allocations: List of allocation dictionaries
        projects_df: Original projects DataFrame
        resources_df: Original resources DataFrame
        output_path: Path to output Excel file
    """
    if not allocations:
        print("Warning: No allocations generated")
        return
    
    allocs_df = pd.DataFrame(allocations)
    
    # Allocations sheet
    allocations_sheet = allocs_df[[
        'resource_id', 'resource_name', 'project_id', 'project_name',
        'month', 'allocated_cost', 'priority_score', 'skill_match_score', 'explanation'
    ]].copy()
    
    # Calculate FTE for allocations (needed for project summary)
    allocs_with_fte = allocs_df.merge(
        resources_df[['brid', 'cost_per_year']],
        left_on='resource_id',
        right_on='brid',
        how='left'
    )
    allocs_with_fte['cost_per_month'] = allocs_with_fte['cost_per_year'] / 12.0
    allocs_with_fte['fte_allocated'] = allocs_with_fte['allocated_cost'] / allocs_with_fte['cost_per_month']
    allocs_with_fte['fte_allocated'] = allocs_with_fte['fte_allocated'].fillna(0.0)
    
    # Project Summary
    project_summary_data = []
    for _, proj_row in projects_df.iterrows():
        pid = int(proj_row['project_id'])
        proj_allocations = allocs_df[allocs_df['project_id'] == pid]
        proj_allocations_with_fte = allocs_with_fte[allocs_with_fte['project_id'] == pid]
        
        allocated_budget = proj_allocations['allocated_cost'].sum()
        allocated_fte = proj_allocations_with_fte['fte_allocated'].sum()
        total_budget = float(proj_row.get('alloc_budget', 0))
        budget_utilization = (allocated_budget / total_budget * 100) if total_budget > 0 else 0.0
        
        priority_score = calculate_project_priority(proj_row, PRIORITY_WEIGHTS)
        months_active = len(set(proj_allocations['month']))
        resources_allocated = len(set(proj_allocations['resource_id']))
        
        effort_estimate = float(proj_row.get('effort_estimate_man_months', 0)) if pd.notna(proj_row.get('effort_estimate_man_months')) else None
        
        project_summary_data.append({
            'project_id': pid,
            'project_name': str(proj_row.get('project_name', f'Project {pid}')),
            'priority_score': priority_score,
            'total_budget': total_budget,
            'allocated_fte': allocated_fte,
            'allocated_cost': allocated_budget,
            'budget_utilization_pct': budget_utilization,
            'months_active': months_active,
            'resources_allocated': resources_allocated,
            'effort_estimate_man_months': effort_estimate,
            'is_efficiency_project': total_budget == 0
        })
    
    project_summary = pd.DataFrame(project_summary_data)
    
    # Resource Summary
    resource_summary_data = []
    for _, resource_row in resources_df.iterrows():
        resource_id = str(resource_row['brid'])
        resource_allocations = allocs_df[allocs_df['resource_id'] == resource_id]
        
        total_allocated_cost = resource_allocations['allocated_cost'].sum()
        annual_cost = float(resource_row.get('cost_per_year', 0))
        monthly_cost = annual_cost / 12.0 if annual_cost > 0 else 0.0
        utilization_pct = (total_allocated_cost / (monthly_cost * 12) * 100) if monthly_cost > 0 else 0.0
        
        # Calculate FTE
        # FTE = allocated_cost / monthly_cost (for each month, then sum)
        total_fte_allocated = 0.0
        if monthly_cost > 0:
            # Sum FTE across all allocations
            total_fte_allocated = resource_allocations['allocated_cost'].sum() / monthly_cost
        
        projects_allocated = len(set(resource_allocations['project_id']))
        months_active = len(set(resource_allocations['month']))
        
        resource_summary_data.append({
            'resource_id': resource_id,
            'resource_name': str(resource_row.get('employee_name', resource_id)),
            'total_allocated_cost': total_allocated_cost,
            'total_fte_allocated': total_fte_allocated,
            'fte_cost_per_month': monthly_cost,
            'annual_cost': annual_cost,
            'utilization_pct': utilization_pct,
            'projects_allocated': projects_allocated,
            'months_active': months_active
        })
    
    resource_summary = pd.DataFrame(resource_summary_data)
    
    # Monthly View - FTE only (pivot table)
    # allocs_with_fte already calculated above for project summary
    monthly_view_fte = allocs_with_fte.pivot_table(
        index=['resource_id', 'resource_name'],
        columns='month',
        values='fte_allocated',
        aggfunc='sum',
        fill_value=0
    ).reset_index()
    
    # Calculate total cost for each resource (sum across all months)
    resource_total_costs = allocs_df.groupby(['resource_id', 'resource_name'])['allocated_cost'].sum().reset_index()
    resource_total_costs.columns = ['resource_id', 'resource_name', 'total_cost']
    
    # Merge FTE view with total cost
    monthly_view = monthly_view_fte.merge(
        resource_total_costs,
        on=['resource_id', 'resource_name'],
        how='left'
    )
    monthly_view['total_cost'] = monthly_view['total_cost'].fillna(0.0)
    
    # Reorder columns: resource info, then months (FTE only), then total_cost at the end
    base_cols = ['resource_id', 'resource_name']
    
    # Get all month columns (sorted)
    month_cols = sorted([col for col in monthly_view.columns 
                        if col not in base_cols and col != 'total_cost'])
    
    # Build ordered column list: resource info, months, then total_cost
    ordered_cols = base_cols + month_cols + ['total_cost']
    
    # Only include columns that exist
    ordered_cols = [col for col in ordered_cols if col in monthly_view.columns]
    monthly_view = monthly_view[ordered_cols]
    
    # Priority Ranking
    priority_ranking = project_summary.sort_values('priority_score', ascending=False)[[
        'project_id', 'project_name', 'priority_score', 'total_budget', 'allocated_fte', 'allocated_cost', 'budget_utilization_pct'
    ]].copy()
    priority_ranking['rank'] = range(1, len(priority_ranking) + 1)
    
    # Skill Gaps - projects that couldn't be fully allocated
    skill_gaps_data = []
    for _, proj_row in projects_df.iterrows():
        pid = int(proj_row['project_id'])
        proj_allocations = allocs_df[allocs_df['project_id'] == pid]
        allocated_budget = proj_allocations['allocated_cost'].sum()
        total_budget = float(proj_row.get('alloc_budget', 0))
        
        if total_budget > 0 and allocated_budget < total_budget * 0.95:  # Less than 95% allocated
            # Check if it's due to skill constraints
            req_skills = parse_required_skills(proj_row.get('required_skills'))
            mandatory_skills = req_skills.get('mandatory', [])
            
            if mandatory_skills:
                # Count resources that can't be allocated due to mandatory skills
                cannot_allocate_count = 0
                for _, resource_row in resources_df.iterrows():
                    can_allocate, _ = check_mandatory_skills(resource_row, req_skills)
                    if not can_allocate:
                        cannot_allocate_count += 1
                
                skill_gaps_data.append({
                    'project_id': pid,
                    'project_name': str(proj_row.get('project_name', f'Project {pid}')),
                    'total_budget': total_budget,
                    'allocated_budget': allocated_budget,
                    'utilization_pct': (allocated_budget / total_budget * 100) if total_budget > 0 else 0.0,
                    'mandatory_skills': ', '.join(mandatory_skills),
                    'resources_without_skills': cannot_allocate_count,
                    'reason': 'Mandatory skills constraint - insufficient resources with required skills'
                })
    
    skill_gaps = pd.DataFrame(skill_gaps_data) if skill_gaps_data else pd.DataFrame(columns=[
        'project_id', 'project_name', 'total_budget', 'allocated_budget', 'utilization_pct',
        'mandatory_skills', 'resources_without_skills', 'reason'
    ])
    
    # Efficiency Projects
    efficiency_projects = project_summary[project_summary['is_efficiency_project'] == True][[
        'project_id', 'project_name', 'allocated_cost', 'allocated_fte', 'resources_allocated', 'months_active'
    ]].copy()
    
    # Write to Excel
    with pd.ExcelWriter(output_path, engine='openpyxl') as writer:
        allocations_sheet.to_excel(writer, sheet_name='Allocations', index=False)
        project_summary.to_excel(writer, sheet_name='Project_Summary', index=False)
        resource_summary.to_excel(writer, sheet_name='Resource_Summary', index=False)
        monthly_view.to_excel(writer, sheet_name='Monthly_View', index=False)
        priority_ranking.to_excel(writer, sheet_name='Priority_Ranking', index=False)
        skill_gaps.to_excel(writer, sheet_name='Skill_Gaps', index=False)
        efficiency_projects.to_excel(writer, sheet_name='Efficiency_Projects', index=False)
    
    print(f"\nOutput generated: {output_path}")
    print(f"  - {len(allocations_sheet)} allocations")
    print(f"  - {len(project_summary)} projects")
    print(f"  - {len(resource_summary)} resources")
    print(f"  - {len(efficiency_projects)} efficiency projects")
    print(f"  - {len(skill_gaps)} projects with skill gaps")


def main():
    """Main execution function."""
    if len(sys.argv) < 2:
        print("Usage: python run_budget_allocator.py <input_excel_file> [output_excel_file]")
        print("\nExample:")
        print("  python run_budget_allocator.py excel/sample_input.xlsx excel/output.xlsx")
        sys.exit(1)
    
    input_path = sys.argv[1]
    output_path = sys.argv[2] if len(sys.argv) > 2 else input_path.replace('.xlsx', '_output.xlsx')
    
    if not Path(input_path).exists():
        print(f"Error: Input file not found: {input_path}")
        sys.exit(1)
    
    print(f"Reading input from: {input_path}")
    
    try:
        # Read input
        projects_df, resources_df = read_input_excel(input_path)
        print(f"  - {len(projects_df)} projects")
        print(f"  - {len(resources_df)} resources")
        
        # Run allocator
        print("\nRunning budget allocator...")
        allocations = budget_allocator(resources_df, projects_df)
        print(f"  - Generated {len(allocations)} allocations")
        
        # Generate output
        print("\nGenerating output...")
        generate_output_excel(allocations, projects_df, resources_df, output_path)
        
        print("\n✓ Allocation complete!")
        
    except Exception as e:
        print(f"\n✗ Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == '__main__':
    main()
