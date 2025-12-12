# Budget Allocator - User Guide

## Getting Started

### Step 1: Prepare Your Data

Create an Excel file with two sheets:

1. **Projects Sheet** - List of projects to allocate resources to
2. **Resources Sheet** - List of available resources

See the template (`excel/budget_allocator_template.xlsx`) for the exact column structure.

### Step 2: Run the Allocator

```bash
cd python
python3 run_budget_allocator.py <input_file.xlsx> [output_file.xlsx]
```

Example:
```bash
python3 run_budget_allocator.py ../excel/sample_input.xlsx ../excel/my_output.xlsx
```

If you don't specify an output file, it will create one with `_output.xlsx` suffix.

### Step 3: Review Results

Open the output Excel file to see:
- **Allocations** - Who is allocated to which project, when, and why
- **Project_Summary** - How well each project was allocated
- **Resource_Summary** - How well each resource was utilized
- **Monthly_View** - Pivot table showing allocations by month
- **Priority_Ranking** - Projects sorted by priority
- **Skill_Gaps** - Projects that couldn't be fully allocated
- **Efficiency_Projects** - Projects with no budget that received allocations

## Understanding the Output

### Allocations Sheet

Each row represents one allocation:
- `resource_id` - Resource BRID
- `resource_name` - Resource name
- `project_id` - Project ID
- `project_name` - Project name
- `month` - Month (YYYY-MM format)
- `allocated_cost` - Cost allocated for this resource-project-month
- `priority_score` - Project priority (0.0 to 1.0)
- `skill_match_score` - How well skills match (0.0 to 1.0)
- `explanation` - Human-readable explanation of why this allocation was made

### Project Summary Sheet

Shows allocation results per project:
- `priority_score` - Calculated priority
- `allocated_budget` - Total cost allocated
- `total_budget` - Original budget
- `budget_utilization_pct` - Percentage of budget used
- `effort_estimate_man_months` - Planned effort
- `is_efficiency_project` - Whether project has no budget

### Resource Summary Sheet

Shows utilization per resource:
- `total_allocated_cost` - Total cost allocated across all projects
- `annual_cost` - Resource annual cost
- `utilization_pct` - Percentage of capacity utilized
- `projects_allocated` - Number of projects allocated to
- `months_active` - Number of months with allocations

## Input Data Requirements

### Projects Sheet Columns

| Column | Required | Description | Example |
|--------|----------|-------------|---------|
| `project_id` | Yes | Unique identifier | 1 |
| `project_name` | Yes | Project name | "Regulatory Project 1" |
| `funding_source` | Yes | Funding source | "Internal" |
| `driver` | Yes | Project driver | "Regulatory", "Product", "Operational" |
| `stakeholder` | Yes | Stakeholder name | "John Doe" |
| `impact` | Yes | Impact level | "High", "Medium", "Low" |
| `rank` | No | Optional rank | 1, 2, 3... |
| `alloc_budget` | Yes | Allocated budget (0 for efficiency) | 100000 |
| `effort_estimate_man_months` | No | Planned effort | 6.0 |
| `start_date` | Yes | Start date | "2025-01" or "2025-01-01" |
| `end_date` | Yes | End date | "2025-06" or "2025-06-30" |
| `required_skills` | Yes | JSON string with skills | See below |
| `comments` | No | Comments | "Important project" |

### Required Skills Format

The `required_skills` column must be a JSON string:

```json
{
  "technical": ["python", "java", "sql"],
  "functional": ["pricing", "risk", "development"],
  "mandatory": ["python"]
}
```

- `technical` - Technical skills (preferred)
- `functional` - Functional skills (preferred)
- `mandatory` - Mandatory skills (hard constraint - resource MUST have all)

### Resources Sheet Columns

| Column | Required | Description | Example |
|--------|----------|-------------|---------|
| `brid` | Yes | Employee BRID | "BRID001" |
| `employee_name` | Yes | Employee name | "Alice Smith" |
| `employee_type` | Yes | Employee type | "Full-time" |
| `role` | Yes | Role | "VP", "AVP", "BA" |
| `role_category` | Yes | Role category | "Vice President" |
| `location` | Yes | Location | "Pune", "Whippany" |
| `grade` | Yes | Grade level | "VP", "AVP", "BA4" |
| `gender` | Yes | Gender | "Male", "Female" |
| `team` | Yes | Team name | "Engineering" |
| `sub_team` | Yes | Sub-team | "Backend" |
| `pod` | Yes | Pod name | "Pod A" |
| `technical_skills` | Yes | Comma-separated | "python,java,sql" |
| `functional_skills` | Yes | Comma-separated | "pricing,risk" |
| `cost_per_year` | Yes | Annual cost | 57000 |

## Priority Calculation

Project priority is calculated using weighted sum:

```
priority = driver_weight × driver_score + impact_weight × impact_score + rank_weight × rank_score
```

Default weights:
- Driver: 40%
- Impact: 40%
- Rank: 20%

Higher priority projects are allocated first (waterfall effect).

## Efficiency Projects

Projects with `alloc_budget = 0` are "efficiency projects". These:
- Have no budget constraint
- Use unallocated resources (resources not fully utilized by budgeted projects)
- Help measure resource utilization efficiency

## Common Issues

### "No allocations generated"

Possible causes:
- No resources have the required mandatory skills
- Budgets are too small
- Project dates are invalid

**Solution**: Check mandatory skills requirements and ensure resources have matching skills.

### "Solver failed"

Possible causes:
- Constraints are too restrictive
- No feasible solution exists

**Solution**: 
- Reduce mandatory skills requirements
- Increase budgets
- Check project dates

### "Low budget utilization"

Possible causes:
- Insufficient resources with required skills
- Budgets too large for available resources
- Mandatory skills too restrictive

**Solution**: Review skill requirements and resource availability.

## Tips for Best Results

1. **Be specific with mandatory skills**: Only mark truly mandatory skills. Too many mandatory skills can prevent allocation.

2. **Set realistic budgets**: Budgets should align with resource costs and effort estimates.

3. **Use effort estimates**: Providing effort estimates helps the allocator make better decisions.

4. **Review explanations**: The explanation column helps understand why each allocation was made.

5. **Check efficiency projects**: These show how well you're utilizing unallocated resources.

## Next Steps

- See [Configuration Guide](CONFIGURATION.md) to customize behavior
- See [Allocation Logic](ALLOCATION_LOGIC.md) to understand the algorithm
