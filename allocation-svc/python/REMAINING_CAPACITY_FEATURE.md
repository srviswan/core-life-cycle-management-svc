# Remaining Capacity Feature

## Overview

All allocators now include **remaining resource capacity** in the allocation results. This shows what resources are still available for future allocations after the current allocation is completed.

## How It Works

After calculating allocations, the system:
1. Tracks total allocated FTE per employee per month
2. Calculates remaining capacity = `fte_capacity - total_allocated`
3. Adds records for any remaining capacity > 0

## Result Format

### Allocation Records
Regular allocation records have:
- `project_id`: Project ID (integer)
- `allocation_fraction`: FTE allocated
- `cost`: Allocation cost
- `available_capacity`: Not set (NaN)

### Available Capacity Records
Remaining capacity records have:
- `project_id`: `None` (or NaN in DataFrame)
- `allocation_fraction`: Remaining FTE available
- `cost`: `0.0` (no cost for available capacity)
- `available_capacity`: `True` (flag to identify available capacity)

## Example Output

```python
allocations = [
    # Actual allocations
    {'scenario_id': 1, 'employee_id': 2, 'project_id': 1, 'month': '2025-01', 
     'allocation_fraction': 0.1, 'cost': 600.0},
    
    # Available capacity
    {'scenario_id': 1, 'employee_id': 1, 'project_id': None, 'month': '2025-01', 
     'allocation_fraction': 1.0, 'cost': 0.0, 'available_capacity': True},
    {'scenario_id': 1, 'employee_id': 2, 'project_id': None, 'month': '2025-01', 
     'allocation_fraction': 0.9, 'cost': 0.0, 'available_capacity': True}
]
```

## Excel Output

The Excel files now include separate sheets:
- **Allocations**: All records (allocations + available capacity)
- **Project_Allocations**: Only actual project allocations
- **Available_Capacity**: Only remaining capacity records

## Usage

### Filter Available Capacity

```python
import pandas as pd

# Load allocations
allocations_df = pd.DataFrame(allocations)

# Get only available capacity
available = allocations_df[
    (allocations_df['available_capacity'] == True) | 
    (allocations_df['project_id'].isna())
]

# Get only actual allocations
actual = allocations_df[allocations_df['project_id'].notna()]
```

### Analyze Available Resources

```python
# Total available FTE
total_available = available['allocation_fraction'].sum()

# Available by employee
by_employee = available.groupby('employee_id')['allocation_fraction'].sum()

# Available by month
by_month = available.groupby('month')['allocation_fraction'].sum()

# Available by employee and month
by_emp_month = available.groupby(['employee_id', 'month'])['allocation_fraction'].sum()
```

## Benefits

1. **Future Planning**: See what resources are available for new projects
2. **Capacity Analysis**: Understand utilization rates
3. **Resource Optimization**: Identify underutilized employees
4. **Budget Planning**: Know available capacity for cost estimation

## Implementation

The allocator includes this feature:
- `fully_optimized_allocator()` in `allocate_fully_optimized.py`

## Notes

- Only months where an employee has at least one allocation are included
- Remaining capacity is calculated as: `capacity - sum(all_allocations_for_month)`
- Skill development allocations are included in the calculation
- Available capacity records have `cost = 0.0` since they represent unused capacity

## Example Analysis

```python
# Example: Find employees with most available capacity
available_by_emp = available.groupby('employee_id')['allocation_fraction'].sum()
print("Employees with most available capacity:")
print(available_by_emp.sort_values(ascending=False))

# Example: Find months with most available capacity
available_by_month = available.groupby('month')['allocation_fraction'].sum()
print("\nMonths with most available capacity:")
print(available_by_month.sort_values(ascending=False))
```

This feature helps with:
- Identifying resources for new project opportunities
- Understanding resource utilization patterns
- Planning future allocations
- Optimizing resource distribution

