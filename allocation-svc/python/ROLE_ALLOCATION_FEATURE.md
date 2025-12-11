# Role-Based Allocation Feature

## Overview

The allocation system now supports **role-based allocation constraints** to ensure every project has proportionate QA (Quality Assurance), DEV (Development), and BA (Business Analyst) resources allocated.

## Features

### 1. Role Detection

The system automatically detects employee roles using one of these methods (in priority order):

1. **Explicit Role Field**: If the `role` column exists in the employee data, it's used directly
2. **Skill-Based Inference**: Roles are inferred from `functional_skills` or `technical_skills`:
   - **QA**: Keywords like "qa", "quality", "testing", "test", "qa engineer", "test engineer"
   - **BA**: Keywords like "ba", "business analyst", "analyst", "business analysis", "requirements"
   - **DEV**: Keywords like "dev", "developer", "development", "engineer", "programming", "coding"
3. **Default**: If no role can be determined, defaults to "DEV"

### 2. Role Allocation Constraints

#### Minimum Role Allocation

Ensures each project-month has minimum FTE allocation for each role:

```python
'min_role_allocation': {
    'DEV': 0.1,   # At least 0.1 FTE of DEV per project-month
    'QA': 0.05,   # At least 0.05 FTE of QA per project-month
    'BA': 0.0     # BA is optional (0 means not required)
}
```

#### Target Role Ratios

Maintains proportional allocation across roles:

```python
'role_allocation_ratios': {
    'DEV': 0.50,  # 50% of project allocation should be DEV
    'QA': 0.30,   # 30% should be QA
    'BA': 0.20    # 20% should be BA (optional)
}
```

## Configuration

### Enable Role Allocation

```python
config = {
    'enforce_role_allocation': True,  # Enable role-based constraints
    'role_allocation_ratios': {
        'DEV': 0.50,
        'QA': 0.30,
        'BA': 0.20
    },
    'min_role_allocation': {
        'DEV': 0.1,
        'QA': 0.05,
        'BA': 0.0  # Optional
    }
}
```

### Weight Configuration

Add `role_balance_weight` to the weights dictionary:

```python
weights = {
    'cost_weight': 0.30,
    'skill_weight': 0.15,
    'fragmentation_weight': 0.10,
    'continuity_weight': 0.10,
    'balance_weight': 0.10,
    'preference_weight': 0.05,
    'diversity_weight': 0.05,
    'leveling_weight': 0.05,
    'role_balance_weight': 0.10  # Weight for role allocation balance
}
```

## Usage Examples

### Example 1: Basic Role Allocation

```python
from allocate_fully_optimized import fully_optimized_allocator

allocations = fully_optimized_allocator(
    employees_df,
    projects_df,
    scenario_id=1,
    config={
        'enforce_role_allocation': True,
        'role_allocation_ratios': {
            'DEV': 0.50,
            'QA': 0.30,
            'BA': 0.20
        },
        'min_role_allocation': {
            'DEV': 0.1,
            'QA': 0.05,
            'BA': 0.0
        }
    },
    weights={
        'role_balance_weight': 0.10
    }
)
```

### Example 2: Custom Ratios

```python
# More DEV-heavy project
config = {
    'enforce_role_allocation': True,
    'role_allocation_ratios': {
        'DEV': 0.70,  # 70% DEV
        'QA': 0.25,   # 25% QA
        'BA': 0.05    # 5% BA
    },
    'min_role_allocation': {
        'DEV': 0.2,
        'QA': 0.1,
        'BA': 0.0
    }
}
```

### Example 3: BA Optional

```python
# BA is optional, only enforce DEV and QA
config = {
    'enforce_role_allocation': True,
    'role_allocation_ratios': {
        'DEV': 0.60,
        'QA': 0.40,
        'BA': 0.0  # Not required
    },
    'min_role_allocation': {
        'DEV': 0.15,
        'QA': 0.10,
        'BA': 0.0  # Not required
    }
}
```

## Employee Data Format

### Option 1: Explicit Role Column

```python
employees_df = pd.DataFrame([
    {
        'employee_id': 1,
        'employee_name': 'Alice',
        'role': 'DEV',  # Explicit role
        'technical_skills': 'python,sql',
        'functional_skills': 'equity swaps',
        'cost_per_month': 12000,
        'fte_capacity': 1.0
    },
    {
        'employee_id': 2,
        'employee_name': 'Bob',
        'role': 'QA',  # Explicit role
        'technical_skills': 'java,selenium',
        'functional_skills': 'testing,quality',
        'cost_per_month': 6000,
        'fte_capacity': 1.0
    },
    {
        'employee_id': 3,
        'employee_name': 'Carol',
        'role': 'BA',  # Explicit role
        'technical_skills': 'sql,excel',
        'functional_skills': 'business analysis',
        'cost_per_month': 8000,
        'fte_capacity': 1.0
    }
])
```

### Option 2: Skill-Based Inference

If no `role` column is provided, roles are inferred from skills:

```python
employees_df = pd.DataFrame([
    {
        'employee_id': 1,
        'employee_name': 'Alice',
        # No role column - will be inferred as 'DEV' from 'development' in functional_skills
        'technical_skills': 'python,sql',
        'functional_skills': 'equity swaps,development',
        'cost_per_month': 12000,
        'fte_capacity': 1.0
    },
    {
        'employee_id': 2,
        'employee_name': 'Bob',
        # Will be inferred as 'QA' from 'testing' in functional_skills
        'technical_skills': 'java,selenium',
        'functional_skills': 'testing,quality assurance',
        'cost_per_month': 6000,
        'fte_capacity': 1.0
    }
])
```

## Output

The allocation output now includes the `employee_role` field:

```python
{
    'scenario_id': 1,
    'employee_id': 1,
    'project_id': 1,
    'month': '2025-01',
    'allocation_fraction': 0.5,
    'cost': 6000.0,
    'employee_name': 'Alice',
    'project_name': 'Alpha',
    'employee_role': 'DEV'  # New field
}
```

## How It Works

### 1. Role Detection Phase

- Employees are categorized into DEV, QA, and BA roles
- Roles are stored in `employees_by_role` dictionary

### 2. Constraint Phase

- **Minimum Allocation Constraints**: For each project-month, ensures minimum FTE for each required role
- **Ratio Balance**: Soft constraints (via penalties) encourage maintaining target ratios

### 3. Objective Function

- **Role Balance Penalty**: Penalizes deviations from target role ratios
- Weighted by `role_balance_weight` in the objective function

## Benefits

1. **Balanced Teams**: Ensures projects have appropriate mix of roles
2. **Quality Assurance**: Guarantees QA resources are allocated
3. **Business Alignment**: Optional BA allocation for requirements and analysis
4. **Flexible Configuration**: Adjustable ratios and minimums per project needs
5. **Automatic Detection**: No need to manually tag every employee

## Limitations

1. **Simplified Ratio Constraints**: Current implementation uses simplified ratio constraints. Full ratio matching would require more complex LP formulation
2. **Role Inference**: Skill-based inference may not always be accurate - prefer explicit role field
3. **Minimum Allocation**: Minimum FTE constraints are hard constraints - if not enough employees of a role exist, solver may be infeasible

## Troubleshooting

### Issue: Solver returns INFEASIBLE

**Possible Causes**:
- Not enough employees of required roles
- Minimum role allocations too high
- Budget constraints too tight

**Solutions**:
1. Reduce `min_role_allocation` values
2. Set BA `min_role_allocation` to 0.0 if BA is optional
3. Increase project budgets
4. Add more employees of required roles

### Issue: Role ratios not being met

**Possible Causes**:
- `role_balance_weight` too low
- Not enough employees of certain roles
- Cost optimization overriding ratio constraints

**Solutions**:
1. Increase `role_balance_weight` (e.g., 0.15 or 0.20)
2. Ensure sufficient employees of each role exist
3. Adjust other weights to balance objectives

## Best Practices

1. **Use Explicit Roles**: Prefer `role` column over skill-based inference
2. **Start with Lower Minimums**: Begin with small minimum allocations (0.05-0.1 FTE)
3. **Adjust Ratios Gradually**: Fine-tune ratios based on project needs
4. **Monitor Output**: Check allocation output to verify role distribution
5. **Balance Weights**: Adjust `role_balance_weight` to balance with other objectives

## Future Enhancements

- Project-specific role requirements
- Role-based skill requirements
- Role allocation reporting and analytics
- Historical role allocation tracking

