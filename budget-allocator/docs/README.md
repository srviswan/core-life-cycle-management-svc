# Budget Allocator

A cost-based resource allocation system that optimally allocates resources to projects based on budgets, priorities, and skill requirements.

## Overview

The Budget Allocator uses linear programming (OR-Tools) to solve the resource allocation problem, ensuring:
- **Cost-based allocation** (not FTE-based)
- **Waterfall priority** - high priority projects allocated first
- **Mandatory skills** - hard constraint that resources must have all mandatory skills
- **Monthly breakdowns** - allocations distributed across project timeline
- **Efficiency projects** - projects with no budget can use unallocated resources
- **Explainable allocations** - each allocation includes a human-readable explanation

## Features

- ✅ Cost-based allocation (up to allocated budget)
- ✅ Yearly with monthly breakdown based on project dates
- ✅ Hard constraint on mandatory skills with AND/OR operators
- ✅ Priority calculation from Driver + Impact + Rank (weighted sum)
- ✅ Waterfall allocation (highest priority first)
- ✅ **Team/Sub-team/Pod Alignment (Hard Constraint)** - Projects with specified teams ONLY allocate resources from that team. Hierarchical fallback: pod → sub_team (within team), never below sub_team
- ✅ **Dummy Resources (New Hires)** - Automatically creates dummy resources for remaining budget, placed in best location with appropriate cost
- ✅ **Driver-based allocation caps** - Projects within a driver fully allocated before moving to other drivers
- ✅ **Funding source prioritization** - Priority and rank considered within each funding_source
- ✅ **Simplified skill system with regex** - Easy-to-use skill matching with AND/OR operators and regex patterns
- ✅ Effort estimate integration
- ✅ Efficiency projects (no budget, uses unallocated resources)
- ✅ Comprehensive output with multiple analysis sheets
- ✅ Allocation explanations for transparency

## Quick Start

### 1. Install Dependencies

```bash
pip install -r requirements.txt
```

### 2. Create Sample Input

```bash
cd python
python3 excel_io.py
```

This creates:
- `excel/budget_allocator_template.xlsx` - Template with sample structure
- `excel/sample_input.xlsx` - Sample with 61 people and 17 projects

### 3. Run Allocator

```bash
python3 run_budget_allocator.py excel/sample_input.xlsx excel/output.xlsx
```

### 4. Review Output

The output Excel file contains:
- **Allocations** - Detailed allocations with explanations
- **Project_Summary** - Budget utilization per project
- **Resource_Summary** - Resource utilization
- **Monthly_View** - Pivot table (resources × months)
- **Priority_Ranking** - Projects sorted by priority
- **Skill_Gaps** - Projects that couldn't be fully allocated
- **Efficiency_Projects** - Projects with no budget that received allocations

## Project Structure

```
budget-allocator/
├── python/
│   ├── budget_allocator.py    # Main allocator engine (OR-Tools)
│   ├── excel_io.py            # Excel template and I/O
│   ├── run_budget_allocator.py # Main execution script
│   ├── utils.py               # Helper functions
│   └── config.py              # Configuration
├── excel/
│   ├── budget_allocator_template.xlsx
│   └── sample_input.xlsx
└── docs/
    ├── README.md (this file)
    ├── USER_GUIDE.md
    ├── CONFIGURATION.md
    └── ALLOCATION_LOGIC.md
```

## Input Format

### Projects Sheet

Required columns:
- `project_id` - Unique identifier
- `project_name` - Project name
- `funding_source` - Funding source
- `driver` - Project driver (for priority)
- `stakeholder` - Stakeholder name
- `impact` - Impact level (High/Medium/Low)
- `rank` - Optional rank (numeric)
- `alloc_budget` - Allocated budget (0 for efficiency projects)
- `effort_estimate_man_months` - Planned effort
- `start_date` - Start date (YYYY-MM or YYYY-MM-DD)
- `end_date` - End date (YYYY-MM or YYYY-MM-DD)
- `team` - Preferred team for allocation (optional)
- `sub_team` - Preferred sub-team for allocation (optional)
- `pod` - Preferred pod for allocation (optional)
- `required_skills` - JSON string with technical, functional, mandatory skills
- `comments` - Comments/notes

### Resources Sheet

Required columns:
- `brid` - Employee BRID (unique identifier)
- `employee_name` - Employee name
- `employee_type` - Employee type
- `role` - Role (DEV/QA/BA/etc.)
- `role_category` - Role category
- `location` - Location
- `grade` - Grade level
- `gender` - Gender
- `team` - Team name
- `sub_team` - Sub-team name
- `pod` - Pod name
- `technical_skills` - Comma-separated technical skills
- `functional_skills` - Comma-separated functional skills
- `cost_per_year` - Annual cost

## How It Works

1. **Priority Calculation**: Projects are ranked using weighted sum of Driver, Impact, and Rank
2. **Allocation Hierarchy**: 
   - **Driver** (highest priority) - Projects within driver fully allocated first
   - **Funding Source** - Priority and rank considered within each funding_source
   - **Project Priority** - Overall project priority
3. **Team/Sub-team/Pod Alignment**: Resources matching project's preferred team/sub_team/pod are strongly preferred (BEFORE skill matching)
4. **Skill Matching**: Resources must meet skill requirements with AND/OR logic (hard constraint). Used as fallback if no team alignment.
5. **Optimization**: OR-Tools solver maximizes allocation with preferences for:
   - Driver caps (waterfall within drivers)
   - Funding source prioritization (waterfall within funding sources)
   - Higher priority projects (waterfall)
   - Team/sub_team/pod alignment (stronger than skill matching)
   - Better skill matches (fallback if no team alignment)
   - Effort estimate alignment
5. **Efficiency Projects**: Projects with no budget use unallocated resources
6. **Explanation Generation**: Each allocation includes a human-readable explanation

## Configuration

See `docs/CONFIGURATION.md` for detailed configuration options.

Key settings in `config.py`:
- Priority weights (driver, impact, rank)
- Waterfall multiplier
- Skill match weight
- Effort estimate weight

## Documentation

- [User Guide](USER_GUIDE.md) - How to use the allocator
- [Configuration Guide](CONFIGURATION.md) - Configuration options
- [Allocation Logic](ALLOCATION_LOGIC.md) - Algorithm details
- [Driver Caps and Skill Operators](DRIVER_CAPS_AND_SKILL_OPERATORS.md) - Driver caps and AND/OR skill logic

## Requirements

- Python 3.8+
- pandas >= 2.0.0
- openpyxl >= 3.1.0
- ortools >= 9.8.0
- numpy >= 1.24.0

## License

Internal use only.
