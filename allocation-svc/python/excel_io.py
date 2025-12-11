"""excel_io.py - create and read Excel template for Budget Planner"""
import pandas as pd, json
from pathlib import Path

def create_template(path):
    employees = pd.DataFrame([
        {'employee_id': 1, 'employee_name': 'Alice', 'status': 'active', 'grade': 'G7', 'location': 'NY', 'country':'USA', 'gender':'female', 'technical_skills':'python,sql', 'functional_skills':'equity swaps,development', 'cost_per_month':12000, 'region':'US', 'fte_capacity':1.0, 'role':'DEV'},
        {'employee_id': 2, 'employee_name': 'Bob', 'status': 'active', 'grade': 'G5', 'location': 'Bengaluru', 'country':'India', 'gender':'male', 'technical_skills':'java,sql', 'functional_skills':'pricing,testing', 'cost_per_month':6000, 'region':'IN', 'fte_capacity':1.0, 'role':'QA'},
        {'employee_id': 3, 'employee_name': 'Carol', 'status': 'active', 'grade': 'G6', 'location': 'London', 'country':'UK', 'gender':'female', 'technical_skills':'sql,excel', 'functional_skills':'business analysis,requirements', 'cost_per_month':8000, 'region':'EU', 'fte_capacity':1.0, 'role':'BA'}
    ])
    projects = pd.DataFrame([
        {'project_id': 1, 'project_name': 'Alpha', 'funding_source':'Internal', 'project_driver':'Regulatory', 'stakeholders':'Alice,Bob', 'impact':'High', 'metrics':'on-time,quality', 'comments':'Important', 'max_budget':30000, 'region_preference':'US', 'required_skills': json.dumps({'technical':['python'],'functional':['pricing']}), 'start_month':'2025-01', 'end_month':'2025-03'},
        {'project_id': 2, 'project_name': 'Beta', 'funding_source':'Grant', 'project_driver':'Product', 'stakeholders':'Carol', 'impact':'Medium', 'metrics':'throughput', 'comments':'Pilot', 'max_budget':25000, 'region_preference':'IN', 'required_skills': json.dumps({'technical':['java'],'functional':['componentX']}), 'start_month':'2025-02', 'end_month':'2025-04'}
    ])
    scenarios = pd.DataFrame([{'scenario_id':1,'scenario_name':'Actual baseline'}])
    allocations = pd.DataFrame(columns=['scenario_id','employee_id','project_id','month','allocation_fraction','cost'])
    comparison = pd.DataFrame(columns=['metric','base_value','scenario_value','delta'])
    with pd.ExcelWriter(path, engine='openpyxl') as writer:
        employees.to_excel(writer, sheet_name='Employees', index=False)
        projects.to_excel(writer, sheet_name='Projects', index=False)
        scenarios.to_excel(writer, sheet_name='Scenarios', index=False)
        allocations.to_excel(writer, sheet_name='Allocations', index=False)
        comparison.to_excel(writer, sheet_name='Comparison', index=False)

if __name__ == '__main__':
    create_template('budget_planner_template.xlsx')

