# Allocation Constraints

This document describes the hard constraints enforced by the allocator.

## Employee Capacity Constraints

### 1. Monthly Capacity Constraint

**Constraint**: Each employee's total allocation across all projects in a single month cannot exceed their `fte_capacity`.

**Formula**: 
```
For each employee e and month m:
  Sum(all allocations for e in m) <= fte_capacity[e]
```

**Example**: 
- Employee has `fte_capacity = 1.0`
- Can be allocated: 0.5 FTE to Project A + 0.5 FTE to Project B = 1.0 FTE total in January ✓
- Cannot be allocated: 0.6 FTE to Project A + 0.5 FTE to Project B = 1.1 FTE total in January ✗

**Implementation**: `Constraint 1` in `allocate_fully_optimized.py` (lines 678-699)

---

### 2. Yearly Average Allocation Constraint (Hard Limit)

**Constraint**: Each employee's average allocation per month across all projects in a calendar year cannot exceed 1.0 FTE.

**Formula**:
```
For each employee e and year y:
  (Sum of all allocations for e across all projects and all months in y) / (number of months in y) <= 1.0 FTE

Equivalently:
  Sum of all allocations for e across all projects and all months in y <= number of months in y
```

**Understanding FTE**:
- 0.5 FTE = 50% of a person working in that month
- 1.0 FTE = 100% of a person working in that month (full-time)
- Average of 1.0 FTE per month means one person fully allocated for the entire year

**Examples**:
- **12 months, fully allocated**: 1.0 FTE in each of 12 months = 12.0 FTE-months total, average = 1.0 FTE/month ✓
- **12 months, part-time**: 0.5 FTE in each of 12 months = 6.0 FTE-months total, average = 0.5 FTE/month ✓
- **12 months, mixed**: 1.0 FTE in Jan, 0.5 FTE in Feb, 0.5 FTE in Mar = 2.0 FTE-months in Q1, average = 0.67 FTE/month ✓
- **12 months, over-allocated**: 1.0 FTE in each of 12 months + 0.1 FTE extra = 12.1 FTE-months total, average = 1.008 FTE/month ✗ (violates constraint)

**Important Notes**:
- This is a **hard constraint** that cannot be violated
- Applies to the sum across **all projects** and **all months** in a calendar year
- Includes both regular allocations and skill development allocations
- The constraint ensures the **average** allocation per month ≤ 1.0 FTE
- If this constraint cannot be satisfied, the solver will return `INFEASIBLE`

**Implementation**: `Constraint 1b` in `allocate_fully_optimized.py` (lines 701-727)

**Why This Constraint?**
- Ensures realistic resource planning (employees cannot average more than 1.0 FTE per month over a year)
- Allows flexibility: can be fully allocated in some months and less in others, as long as average ≤ 1.0
- Prevents over-allocation across multiple projects throughout the year
- Aligns with typical workforce management practices where one person = 1.0 FTE per month on average

---

## Budget Constraints

### 3. Project Budget Constraint

**Constraint**: Total cost of allocations for a project cannot exceed the project's `max_budget`.

**Formula**:
```
For each project p:
  Sum(cost of all allocations for p) <= max_budget[p]
```

**Implementation**: `Constraint 2` in `allocate_fully_optimized.py`

---

## Team Size Constraints

### 4. Minimum Team Size Constraint

**Constraint**: Each project must have at least `min_team_size` employees (or FTE equivalent) allocated per month.

**Formula**:
```
For each project p and month m:
  Sum(all allocations for p in m) >= min_team_size
```

**Implementation**: `Constraint 3` in `allocate_fully_optimized.py`

---

## Role-Based Constraints

### 5. Minimum Role Allocation Constraint

**Constraint**: If `enforce_role_allocation` is enabled, each project must have minimum FTE allocation per role.

**Formula**:
```
For each project p, month m, and role r:
  Sum(allocations for employees with role r to p in m) >= min_role_allocation[r]
```

**Implementation**: `Constraint 4` in `allocate_fully_optimized.py`

---

## Constraint Interactions

### Monthly vs Yearly Capacity

The monthly and yearly capacity constraints work together:

- **Monthly constraint** (`fte_capacity`): Limits allocation per month (typically 1.0 FTE)
- **Yearly constraint** (hard limit): Limits average allocation per month to 1.0 FTE over the year

**Example Scenarios**:

**Scenario 1: Fully allocated for entire year**
- Employee with `fte_capacity = 1.0`
- Monthly constraint allows: 1.0 FTE in each month
- Yearly constraint allows: 1.0 FTE × 12 months = 12.0 FTE-months total, average = 1.0 FTE/month ✓

**Scenario 2: Part-time for entire year**
- Employee with `fte_capacity = 1.0`
- Monthly constraint allows: 0.5 FTE in each month
- Yearly constraint allows: 0.5 FTE × 12 months = 6.0 FTE-months total, average = 0.5 FTE/month ✓

**Scenario 3: Variable allocation**
- Employee with `fte_capacity = 1.0`
- Monthly constraint allows: 1.0 FTE in Jan, 0.5 FTE in Feb, 0.5 FTE in Mar, etc.
- Yearly constraint allows: As long as total across 12 months ≤ 12.0 FTE-months (average ≤ 1.0 FTE/month) ✓

**Scenario 4: Over-allocation (violates constraint)**
- Employee allocated 1.0 FTE in each of 12 months + 0.1 FTE extra = 12.1 FTE-months
- Average = 12.1 / 12 = 1.008 FTE/month ✗ (violates yearly constraint)

### Solver Infeasibility

If constraints cannot be satisfied, the solver will return `INFEASIBLE`. Common causes:

1. **Yearly constraint violation**: Too many projects requiring the same employees
2. **Budget too small**: Project budgets insufficient for required allocations
3. **Team size too large**: `min_team_size` requires more employees than available
4. **Role constraints too strict**: `min_role_allocation` requires more employees of specific roles than available

### Recommendations

1. **Monitor yearly totals**: Check that employee allocations don't exceed 1.0 FTE per year
2. **Adjust project timelines**: Spread projects across different time periods to avoid conflicts
3. **Increase budgets**: Ensure project budgets are sufficient for required allocations
4. **Relax constraints**: Consider reducing `min_team_size` or `min_role_allocation` if solver is infeasible

---

**Last Updated**: Based on `allocate_fully_optimized.py` implementation
**Version**: 1.0
