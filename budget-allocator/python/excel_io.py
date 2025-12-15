"""excel_io.py - Excel template creation and I/O for budget allocator"""
import pandas as pd
import json
from pathlib import Path
from typing import Optional


def create_template(output_path: str):
    """Create Excel template with Projects and Resources sheets.
    
    Args:
        output_path: Path to output Excel file
    """
    # Projects template with all required columns
    projects = pd.DataFrame([
        {
            'project_id': 1,
            'project_name': 'Sample Project Alpha',
            'funding_source': 'Internal',
            'driver': 'Regulatory',
            'stakeholder': 'John Doe',
            'impact': 'High',
            'rank': 1,
            'requested_budget': 50000,  # Bottom-up budget request (what project asks for)
            'alloc_budget': 50000,  # Maximum budget that can be allocated (constraint)
            'effort_estimate_man_months': 6.0,
            'start_date': '2025-01',
            'end_date': '2025-06',
            'required_skills': 'python,sql|java',  # Simple format: python AND sql OR java
            # Alternative JSON format (legacy, still supported):
            # json.dumps({
            #     'mandatory_and': ['python'],
            #     'mandatory_or': ['java', 'c++'],
            #     'technical_and': ['sql'],
            #     'technical_or': ['pandas'],
            #     'functional_and': ['pricing'],
            #     'functional_or': ['risk', 'trading']
            # })
            # Supports regex: 'regex:python.*|java' or 'python*|java'
            'max_resource_allocation_pct': 1.0,  # Max % of resource capacity per month (0.0-1.0, default 1.0 = 100%)
            'comments': 'Sample project for testing'
        }
    ])
    
    # Resources template with all required columns
    resources = pd.DataFrame([
        {
            'brid': 'BRID001',
            'employee_name': 'Alice Smith',
            'employee_type': 'Full-time',
            'role': 'DEV',
            'role_category': 'Developer',
            'location': 'Pune',
            'grade': 'VP',
            'gender': 'Female',
            'team': 'Engineering',
            'sub_team': 'Backend',
            'pod': 'Pod A',
            'technical_skills': 'python,java,sql',
            'functional_skills': 'pricing,risk,development',
            'cost_per_year': 57000
        }
    ])
    
    # Config sheet (optional)
    config_data = pd.DataFrame([
        {'parameter': 'driver_weight', 'value': 0.4},
        {'parameter': 'impact_weight', 'value': 0.4},
        {'parameter': 'rank_weight', 'value': 0.2},
    ])
    
    with pd.ExcelWriter(output_path, engine='openpyxl') as writer:
        projects.to_excel(writer, sheet_name='Projects', index=False)
        resources.to_excel(writer, sheet_name='Resources', index=False)
        config_data.to_excel(writer, sheet_name='Config', index=False)
    
    print(f"Template created: {output_path}")


def create_sample_input(output_path: str):
    """Create sample input Excel with 61 people and 17 projects.
    
    Resources:
    - Pune, India (25): 3 VP (57K), 11 AVP (38K), 11 BA4 (28K)
    - USA Whippany (12): 7 VP (198K), 3 AVP (160K), 2 BA4 (90K)
    - Prague (9): 2 VP (160K), 5 AVP (90K), 2 BA4 (60K)
    - London (3): 1 VP (180K), 2 AVP (140K)
    - HK (12): 2 VP (160K), 10 AVP (140K)
    
    Args:
        output_path: Path to output Excel file
    """
    resources_data = []
    resource_id = 1
    
    # Pune, India (25 people)
    for i in range(3):  # 3 VP
        resources_data.append({
            'brid': f'PUNE_VP_{i+1:03d}',
            'employee_name': f'Pune VP {i+1}',
            'employee_type': 'Full-time',
            'role': 'VP',
            'role_category': 'Vice President',
            'location': 'Pune',
            'grade': 'VP',
            'gender': 'Male' if i % 2 == 0 else 'Female',
            'team': 'Engineering',
            'sub_team': 'Platform',
            'pod': 'Pod A',
            'technical_skills': 'python,java,architecture,design',
            'functional_skills': 'strategy,leadership,planning',
            'cost_per_year': 57000
        })
    
    for i in range(11):  # 11 AVP
        resources_data.append({
            'brid': f'PUNE_AVP_{i+1:03d}',
            'employee_name': f'Pune AVP {i+1}',
            'employee_type': 'Full-time',
            'role': 'AVP',
            'role_category': 'Associate Vice President',
            'location': 'Pune',
            'grade': 'AVP',
            'gender': 'Male' if i % 2 == 0 else 'Female',
            'team': 'Engineering',
            'sub_team': 'Backend' if i < 6 else 'Frontend',
            'pod': 'Pod A' if i < 6 else 'Pod B',
            'technical_skills': 'java,python,sql,spring',
            'functional_skills': 'development,testing,analysis',
            'cost_per_year': 38000
        })
    
    for i in range(11):  # 11 BA4
        resources_data.append({
            'brid': f'PUNE_BA4_{i+1:03d}',
            'employee_name': f'Pune BA4 {i+1}',
            'employee_type': 'Full-time',
            'role': 'BA',
            'role_category': 'Business Analyst',
            'location': 'Pune',
            'grade': 'BA4',
            'gender': 'Male' if i % 2 == 0 else 'Female',
            'team': 'Business Analysis',
            'sub_team': 'Requirements',
            'pod': 'Pod C',
            'technical_skills': 'sql,excel,python',
            'functional_skills': 'requirements,analysis,documentation',
            'cost_per_year': 28000
        })
    
    # USA Whippany (12 people)
    for i in range(7):  # 7 VP
        resources_data.append({
            'brid': f'USA_VP_{i+1:03d}',
            'employee_name': f'USA VP {i+1}',
            'employee_type': 'Full-time',
            'role': 'VP',
            'role_category': 'Vice President',
            'location': 'Whippany',
            'grade': 'VP',
            'gender': 'Male' if i % 2 == 0 else 'Female',
            'team': 'Engineering',
            'sub_team': 'Platform',
            'pod': 'Pod D',
            'technical_skills': 'java,python,architecture,cloud',
            'functional_skills': 'strategy,leadership,planning',
            'cost_per_year': 198000
        })
    
    for i in range(3):  # 3 AVP
        resources_data.append({
            'brid': f'USA_AVP_{i+1:03d}',
            'employee_name': f'USA AVP {i+1}',
            'employee_type': 'Full-time',
            'role': 'AVP',
            'role_category': 'Associate Vice President',
            'location': 'Whippany',
            'grade': 'AVP',
            'gender': 'Male' if i % 2 == 0 else 'Female',
            'team': 'Engineering',
            'sub_team': 'Backend',
            'pod': 'Pod D',
            'technical_skills': 'java,python,sql,spring',
            'functional_skills': 'development,testing',
            'cost_per_year': 160000
        })
    
    for i in range(2):  # 2 BA4
        resources_data.append({
            'brid': f'USA_BA4_{i+1:03d}',
            'employee_name': f'USA BA4 {i+1}',
            'employee_type': 'Full-time',
            'role': 'BA',
            'role_category': 'Business Analyst',
            'location': 'Whippany',
            'grade': 'BA4',
            'gender': 'Male' if i % 2 == 0 else 'Female',
            'team': 'Business Analysis',
            'sub_team': 'Requirements',
            'pod': 'Pod D',
            'technical_skills': 'sql,excel',
            'functional_skills': 'requirements,analysis',
            'cost_per_year': 90000
        })
    
    # Prague (9 people)
    for i in range(2):  # 2 VP
        resources_data.append({
            'brid': f'PRAGUE_VP_{i+1:03d}',
            'employee_name': f'Prague VP {i+1}',
            'employee_type': 'Full-time',
            'role': 'VP',
            'role_category': 'Vice President',
            'location': 'Prague',
            'grade': 'VP',
            'gender': 'Male' if i % 2 == 0 else 'Female',
            'team': 'Engineering',
            'sub_team': 'Platform',
            'pod': 'Pod E',
            'technical_skills': 'java,python,architecture',
            'functional_skills': 'strategy,leadership',
            'cost_per_year': 160000
        })
    
    for i in range(5):  # 5 AVP
        resources_data.append({
            'brid': f'PRAGUE_AVP_{i+1:03d}',
            'employee_name': f'Prague AVP {i+1}',
            'employee_type': 'Full-time',
            'role': 'AVP',
            'role_category': 'Associate Vice President',
            'location': 'Prague',
            'grade': 'AVP',
            'gender': 'Male' if i % 2 == 0 else 'Female',
            'team': 'Engineering',
            'sub_team': 'Backend',
            'pod': 'Pod E',
            'technical_skills': 'java,python,sql',
            'functional_skills': 'development,testing',
            'cost_per_year': 90000
        })
    
    for i in range(2):  # 2 BA4
        resources_data.append({
            'brid': f'PRAGUE_BA4_{i+1:03d}',
            'employee_name': f'Prague BA4 {i+1}',
            'employee_type': 'Full-time',
            'role': 'BA',
            'role_category': 'Business Analyst',
            'location': 'Prague',
            'grade': 'BA4',
            'gender': 'Male' if i % 2 == 0 else 'Female',
            'team': 'Business Analysis',
            'sub_team': 'Requirements',
            'pod': 'Pod E',
            'technical_skills': 'sql,excel',
            'functional_skills': 'requirements,analysis',
            'cost_per_year': 60000
        })
    
    # London (3 people)
    resources_data.append({
        'brid': 'LONDON_VP_001',
        'employee_name': 'London VP 1',
        'employee_type': 'Full-time',
        'role': 'VP',
        'role_category': 'Vice President',
        'location': 'London',
        'grade': 'VP',
        'gender': 'Male',
        'team': 'Engineering',
        'sub_team': 'Platform',
        'pod': 'Pod F',
        'technical_skills': 'java,python,architecture',
        'functional_skills': 'strategy,leadership',
        'cost_per_year': 180000
    })
    
    for i in range(2):  # 2 AVP
        resources_data.append({
            'brid': f'LONDON_AVP_{i+1:03d}',
            'employee_name': f'London AVP {i+1}',
            'employee_type': 'Full-time',
            'role': 'AVP',
            'role_category': 'Associate Vice President',
            'location': 'London',
            'grade': 'AVP',
            'gender': 'Male' if i % 2 == 0 else 'Female',
            'team': 'Engineering',
            'sub_team': 'Backend',
            'pod': 'Pod F',
            'technical_skills': 'java,python,sql',
            'functional_skills': 'development,testing',
            'cost_per_year': 140000
        })
    
    # HK (12 people)
    for i in range(2):  # 2 VP
        resources_data.append({
            'brid': f'HK_VP_{i+1:03d}',
            'employee_name': f'HK VP {i+1}',
            'employee_type': 'Full-time',
            'role': 'VP',
            'role_category': 'Vice President',
            'location': 'HK',
            'grade': 'VP',
            'gender': 'Male' if i % 2 == 0 else 'Female',
            'team': 'Engineering',
            'sub_team': 'Platform',
            'pod': 'Pod G',
            'technical_skills': 'java,python,architecture',
            'functional_skills': 'strategy,leadership',
            'cost_per_year': 160000
        })
    
    for i in range(10):  # 10 AVP
        resources_data.append({
            'brid': f'HK_AVP_{i+1:03d}',
            'employee_name': f'HK AVP {i+1}',
            'employee_type': 'Full-time',
            'role': 'AVP',
            'role_category': 'Associate Vice President',
            'location': 'HK',
            'grade': 'AVP',
            'gender': 'Male' if i % 2 == 0 else 'Female',
            'team': 'Engineering',
            'sub_team': 'Backend' if i < 5 else 'Frontend',
            'pod': 'Pod G',
            'technical_skills': 'java,python,sql',
            'functional_skills': 'development,testing',
            'cost_per_year': 140000
        })
    
    resources_df = pd.DataFrame(resources_data)
    
    # Projects (17 projects with mix of budgets and requirements)
    projects_data = []
    
    # High priority projects with budgets
    for i in range(1, 8):
        projects_data.append({
            'project_id': i,
            'project_name': f'Regulatory Project {i}',
            'funding_source': 'Internal',
            'driver': 'Regulatory',
            'stakeholder': f'Stakeholder {i}',
            'impact': 'High',
            'rank': i,
            'requested_budget': 120000 + i * 12000,  # Bottom-up ask (higher than alloc_budget)
            'alloc_budget': 100000 + i * 10000,  # Approved budget cap
            'effort_estimate_man_months': 6.0 + i * 0.5,
            'start_date': '2025-01',
            'end_date': '2025-06',
            'required_skills': json.dumps({
                'technical': ['python', 'java'] if i % 2 == 0 else ['java', 'sql'],
                'functional': ['pricing', 'risk'],
                'mandatory': ['python'] if i % 2 == 0 else ['java']
            }),
            'comments': f'High priority regulatory project {i}'
        })
    
    # Medium priority projects
    for i in range(8, 13):
        projects_data.append({
            'project_id': i,
            'project_name': f'Product Project {i-7}',
            'funding_source': 'Internal',
            'driver': 'Product',
            'stakeholder': f'Stakeholder {i}',
            'impact': 'Medium',
            'rank': i,
            'requested_budget': 60000 + (i-8) * 6000,  # Bottom-up ask
            'alloc_budget': 50000 + (i-8) * 5000,  # Approved budget cap
            'effort_estimate_man_months': 4.0 + (i-8) * 0.3,
            'start_date': '2025-02',
            'end_date': '2025-08',
            'required_skills': json.dumps({
                'technical': ['sql', 'python'],
                'functional': ['development', 'testing'],
                'mandatory': ['sql']
            }),
            'comments': f'Medium priority product project {i-7}'
        })
    
    # Efficiency projects (no budget)
    for i in range(13, 18):
        projects_data.append({
            'project_id': i,
            'project_name': f'Efficiency Project {i-12}',
            'funding_source': 'Internal',
            'driver': 'Operational',
            'stakeholder': f'Stakeholder {i}',
            'impact': 'Low',
            'rank': i,
            'requested_budget': 0,  # No budget request - efficiency project
            'alloc_budget': 0,  # No budget - efficiency project
            'effort_estimate_man_months': 3.0 + (i-13) * 0.2,
            'start_date': '2025-03',
            'end_date': '2025-09',
            'required_skills': json.dumps({
                'technical': ['python', 'java'],
                'functional': ['analysis'],
                'mandatory': []
            }),
            'max_resource_allocation_pct': 1.0,  # No limit for efficiency projects
            'comments': f'Efficiency project {i-12} - uses unallocated resources'
        })
    
    projects_df = pd.DataFrame(projects_data)
    
    # Write to Excel
    with pd.ExcelWriter(output_path, engine='openpyxl') as writer:
        projects_df.to_excel(writer, sheet_name='Projects', index=False)
        resources_df.to_excel(writer, sheet_name='Resources', index=False)
    
    print(f"Sample input created: {output_path}")
    print(f"  - {len(resources_df)} resources")
    print(f"  - {len(projects_df)} projects")
    print(f"  - {len([p for p in projects_data if p['alloc_budget'] == 0])} efficiency projects (no budget)")


if __name__ == '__main__':
    # Create template
    template_path = Path(__file__).parent.parent / 'excel' / 'budget_allocator_template.xlsx'
    template_path.parent.mkdir(parents=True, exist_ok=True)
    create_template(str(template_path))
    
    # Create sample input
    sample_path = Path(__file__).parent.parent / 'excel' / 'sample_input.xlsx'
    create_sample_input(str(sample_path))
