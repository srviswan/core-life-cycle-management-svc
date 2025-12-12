# Project Role Restrictions Guide

## Overview

You can enforce that certain projects only allow specific roles to be allocated. For example, a project might only allow Business Analysts (BA) and no Developers (DEV) or QA Engineers.

## How It Works

When a project specifies `allowed_roles`, the allocator will:
1. **Skip variable creation** for employees with disallowed roles (efficient)
2. **Add hard constraints** to ensure no allocation to disallowed roles (safety)
3. Only employees with the specified roles can be allocated to that project

## Usage

### In Project Data

Add `allowed_roles` or `required_roles` field to your project data:

#### Example 1: Only BA Allowed

```python
projects = pd.DataFrame([
    {
        'project_id': 1,
        'project_name': 'Business Analysis Project',
        'allowed_roles': 'BA',  # Only Business Analysts
        'max_budget': 50000,
        ...
    }
])
```

#### Example 2: Multiple Roles Allowed

```python
projects = pd.DataFrame([
    {
        'project_id': 2,
        'project_name': 'Analysis and Development',
        'allowed_roles': 'BA,DEV',  # BA and DEV only, no QA
        'max_budget': 100000,
        ...
    }
])
```

#### Example 3: Using List Format

```python
projects = pd.DataFrame([
    {
        'project_id': 3,
        'project_name': 'Quality Assurance Project',
        'allowed_roles': ['QA'],  # List format also works
        'max_budget': 75000,
        ...
    }
])
```

### In Excel Template

If using Excel templates, add an `allowed_roles` column:

| project_id | project_name | allowed_roles | max_budget | ... |
|------------|--------------|---------------|------------|-----|
| 1 | BA Only Project | BA | 50000 | ... |
| 2 | Dev+BA Project | BA,DEV | 100000 | ... |
| 3 | QA Only Project | QA | 75000 | ... |

## Role Name Formats

The system accepts various role name formats and normalizes them:

### Supported Formats

- **Standard**: `BA`, `DEV`, `QA`
- **Variations**: `Business Analyst`, `Developer`, `Tester`, `Quality`
- **Case insensitive**: `ba`, `BA`, `Ba` all work
- **Comma-separated**: `BA,DEV` or `BA, DEV` (spaces are trimmed)

### Role Mapping

The system automatically maps common variations:
- `DEVELOPER`, `DEVELOPERS`, `DEV` → `DEV`
- `TESTER`, `TESTERS`, `QUALITY`, `QA` → `QA`
- `BUSINESS ANALYST`, `BUSINESS ANALYSTS`, `ANALYST`, `BA` → `BA`

## Examples

### Example 1: BA-Only Project

```python
projects = pd.DataFrame([
    {
        'project_id': 1,
        'project_name': 'Requirements Analysis',
        'allowed_roles': 'BA',
        'max_budget': 50000,
        'start_month': '2025-01',
        'end_month': '2025-06',
        'required_skills': json.dumps({'technical': [], 'functional': ['requirements']})
    }
])
```

**Result:** Only employees with role `BA` can be allocated to this project.

### Example 2: DEV and QA Only (No BA)

```python
projects = pd.DataFrame([
    {
        'project_id': 2,
        'project_name': 'Development and Testing',
        'allowed_roles': 'DEV,QA',
        'max_budget': 150000,
        ...
    }
])
```

**Result:** Only `DEV` and `QA` employees can be allocated. `BA` employees are excluded.

### Example 3: All Roles Allowed (Default)

```python
projects = pd.DataFrame([
    {
        'project_id': 3,
        'project_name': 'General Project',
        # No allowed_roles specified
        'max_budget': 100000,
        ...
    }
])
```

**Result:** All roles (`DEV`, `QA`, `BA`) can be allocated (default behavior).

## Implementation Details

### Variable Creation

When `allowed_roles` is specified:
- Variables are **not created** for employees with disallowed roles
- This is efficient and prevents unnecessary solver variables

### Constraints

As a safety measure, hard constraints are also added:
- Allocation variables for disallowed roles are set to 0
- This ensures no allocation even if variables exist

### Role Detection

Employee roles are determined from:
1. Explicit `role` field in employee data
2. Inferred from skills (if role field missing)
3. Defaults to `DEV` if no role can be determined

## Best Practices

1. **Be Specific:**
   - Only specify `allowed_roles` when you need restrictions
   - Leave it empty/unspecified for projects that accept all roles

2. **Check Employee Roles:**
   - Ensure employees have correct `role` field set
   - Verify role inference is working correctly

3. **Combine with Other Constraints:**
   - Role restrictions work with other constraints (budget, skills, etc.)
   - Can be combined with `enforce_role_allocation` for additional control

4. **Monitor Results:**
   - Check allocation output to verify only allowed roles are allocated
   - If a project has no allocations, check if allowed roles are too restrictive

## Troubleshooting

**Issue:** Project has no allocations even though employees are available
- **Solution:** Check if `allowed_roles` is too restrictive or if employees don't have matching roles

**Issue:** Wrong roles being allocated
- **Solution:** Verify employee `role` field is set correctly and matches `allowed_roles`

**Issue:** Solver infeasibility
- **Solution:** Role restrictions are hard constraints. If too restrictive, consider:
  - Expanding `allowed_roles` to include more roles
  - Ensuring there are enough employees with allowed roles
  - Checking other constraints (budget, capacity, etc.)

## Example: Complete Project Definition

```python
projects = pd.DataFrame([
    {
        'project_id': 1,
        'project_name': 'BA Analysis Project',
        'allowed_roles': 'BA',  # Only BA allowed
        'max_budget': 50000,
        'start_month': '2025-01',
        'end_month': '2025-12',
        'required_skills': json.dumps({
            'technical': [],
            'functional': ['requirements', 'analysis']
        }),
        'impact': 'High',
        'priority': 1
    },
    {
        'project_id': 2,
        'project_name': 'Development Project',
        'allowed_roles': 'DEV,QA',  # DEV and QA only
        'max_budget': 150000,
        'start_month': '2025-01',
        'end_month': '2025-12',
        'required_skills': json.dumps({
            'technical': ['java', 'python'],
            'functional': ['development']
        }),
        'impact': 'High',
        'priority': 1
    },
    {
        'project_id': 3,
        'project_name': 'General Project',
        # No allowed_roles - all roles allowed
        'max_budget': 100000,
        'start_month': '2025-01',
        'end_month': '2025-12',
        'required_skills': json.dumps({
            'technical': ['java'],
            'functional': ['general']
        }),
        'impact': 'Medium',
        'priority': 1
    }
])
```
