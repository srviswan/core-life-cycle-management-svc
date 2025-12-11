# Complete Configuration Guide for Fully Optimized Allocator

## Overview

This document provides a comprehensive guide to all configuration options available in the `fully_optimized_allocator()` function, including their relationships, mutual exclusivity, impacts, and side effects.

---

## Table of Contents

1. [Configuration Options](#configuration-options)
2. [Weight Options](#weight-options)
3. [Mutual Exclusivity & Dependencies](#mutual-exclusivity--dependencies)
4. [Impact Analysis](#impact-analysis)
5. [Side Effects & Interactions](#side-effects--interactions)
6. [Recommended Configurations](#recommended-configurations)

---

## Configuration Options

### 1. Resource Allocation Constraints

#### `max_employee_per_project` (float, default: 0.8)
**Description**: Maximum FTE allocation per employee per project per month.

**Range**: 0.0 to 1.0 (typically)

**Impact**:
- Limits single-employee dependency risk
- Prevents over-allocation of one employee to a project
- Lower values = more distributed allocations

**Side Effects**:
- May reduce budget utilization if set too low
- Interacts with `min_team_size` - may require more employees to meet team size
- Affects `maximize_budget_utilization` - lower values limit maximum allocation

**Example**:
```python
config = {'max_employee_per_project': 0.5}  # Max 50% FTE per employee per project
```

---

#### `min_team_size` (int, default: 1)
**Description**: Minimum number of employees (or FTE equivalent) per project per month.

**Range**: 0 or positive integer

**Impact**:
- Ensures projects have sufficient team size
- Prevents single-person projects (if set > 1)
- Adds constraint: `sum(allocations) >= min_team_size * 0.1 FTE`

**Side Effects**:
- May cause infeasibility if budget is too small
- Interacts with `max_employee_per_project` - may need more employees
- Higher values reduce flexibility in allocation

**Example**:
```python
config = {'min_team_size': 2}  # Require at least 2 team members
```

---

### 2. Skill Development

#### `allow_skill_development` (bool, default: True)
**Description**: Allow employees to be allocated to projects for skill development even if they lack required skills.

**Impact**:
- Enables learning allocations for employees with partial skills
- Creates separate `skill_dev_variables` in the solver
- Increases solution space (more allocation possibilities)

**Side Effects**:
- Interacts with `allow_allocation_without_skills`:
  - If both True: skill dev for partial skills, no-skills allocation for no skills
  - If skill_dev=False but no_skills=True: only no-skills allocations allowed
- Affects skill quality scoring (skill dev allocations have lower scores)
- Uses `skill_dev_max_fte` limit

**Mutual Exclusivity**: None (can work with `allow_allocation_without_skills`)

**Example**:
```python
config = {'allow_skill_development': True}
```

---

#### `skill_dev_max_fte` (float, default: 0.2)
**Description**: Maximum FTE allocation for skill development per employee per project per month.

**Range**: 0.0 to 1.0

**Impact**:
- Limits how much time employees spend learning vs. productive work
- Prevents over-allocation to skill development

**Side Effects**:
- Only active if `allow_skill_development: True`
- Lower values = less skill development, more productive allocations
- Counts toward employee capacity constraints

**Dependencies**: Requires `allow_skill_development: True`

**Example**:
```python
config = {
    'allow_skill_development': True,
    'skill_dev_max_fte': 0.1  # Max 10% FTE for learning
}
```

---

### 3. Discrete vs Continuous Allocations

#### `discrete_allocations` (bool, default: False)
**Description**: Use discrete allocation increments instead of continuous values.

**Impact**:
- Changes solver from GLOP (linear) to SCIP/CBC (integer programming)
- Forces allocations to specific increments (e.g., 0.25, 0.5, 0.75, 1.0)
- More realistic for resource planning (people work in chunks)

**Side Effects**:
- **Solver Performance**: Integer solvers are slower than linear solvers
- **Solution Quality**: May find suboptimal solutions due to discrete constraints
- **Budget Utilization**: May reduce budget usage (can't use exact amounts)
- Affects all constraint coefficients (uses increment values instead of 1.0)
- Changes how `max_employee_per_project` is enforced (uses level limits)

**Mutual Exclusivity**: 
- Mutually exclusive with continuous allocations (either discrete or continuous)

**Dependencies**: Requires `allocation_increments` list

**Example**:
```python
config = {
    'discrete_allocations': True,
    'allocation_increments': [0.25, 0.5, 0.75, 1.0]
}
```

---

#### `allocation_increments` (list, default: [0.25, 0.5, 0.75, 1.0])
**Description**: List of allowed FTE increments when using discrete allocations.

**Range**: Values between 0.0 and 1.0

**Impact**:
- Defines possible allocation levels
- Solver chooses which increment level to use (0 = no allocation, 1 = first increment, etc.)

**Side Effects**:
- Only used when `discrete_allocations: True`
- Affects minimum allocation constraints (uses `min(increments)`)
- Affects capacity constraints (uses `max(increments)`)
- More increments = more flexibility but slower solver

**Dependencies**: Requires `discrete_allocations: True`

**Example**:
```python
config = {
    'discrete_allocations': True,
    'allocation_increments': [0.1, 0.2, 0.5, 1.0]  # Custom increments
}
```

---

### 4. Budget Management

#### `budget_flexibility` (bool, default: True)
**Description**: Allow budget borrowing between months within a project.

**Impact**:
- **True**: Total budget constraint across all months (can overspend in some months)
- **False**: Per-month budget constraints (must stay within monthly budget each month)

**Side Effects**:
- **True**: More flexible, better budget utilization, allows front-loading/back-loading
- **False**: More restrictive, may reduce allocation if monthly budgets are tight
- Affects `min_budget_utilization` constraint (applies to total vs. per-month)
- Interacts with `max_budget_borrow` (if implemented)

**Mutual Exclusivity**: 
- Mutually exclusive approaches (total budget vs. per-month budget)

**Example**:
```python
config = {'budget_flexibility': False}  # Strict per-month budgets
```

---

#### `max_budget_borrow` (int, default: 1)
**Description**: Maximum number of months to borrow budget from (future implementation).

**Note**: Currently not fully implemented in constraints, but reserved for future use.

---

#### `maximize_budget_utilization` (bool, default: False)
**Description**: Maximize budget usage instead of minimizing cost.

**Impact**:
- Adds negative cost term to objective function
- Prioritizes using available budget over cost savings
- Weight = `cost_weight * budget_maximization_weight_multiplier`

**Side Effects**:
- **Conflicts with cost minimization**: Both are in objective, but budget maximization can dominate if weight is high enough
- Higher `budget_maximization_weight_multiplier` = stronger budget maximization
- May result in higher costs (uses more expensive employees if needed)
- Works with `min_budget_utilization` constraint

**Mutual Exclusivity**: 
- Not mutually exclusive, but **conflicting objectives**:
  - Cost minimization (weight: 0.30) vs. Budget maximization (weight: 0.30 * multiplier)
  - If multiplier < 1.0: cost minimization wins
  - If multiplier > 1.0: budget maximization wins

**Dependencies**: 
- Works with `budget_maximization_weight_multiplier`
- Can be combined with `min_budget_utilization` for guaranteed minimum usage

**Example**:
```python
config = {
    'maximize_budget_utilization': True,
    'budget_maximization_weight_multiplier': 1.5  # 1.5x stronger than cost minimization
}
```

---

#### `budget_maximization_weight_multiplier` (float, default: 1.0)
**Description**: Multiplier for budget maximization weight relative to cost weight.

**Range**: 0.0 to any positive value

**Impact**:
- Controls strength of budget maximization vs. cost minimization
- Weight = `cost_weight * multiplier`
- Multiplier > 1.0: Budget maximization stronger
- Multiplier < 1.0: Cost minimization stronger

**Side Effects**:
- Only active if `maximize_budget_utilization: True`
- Higher values = more budget usage, potentially higher costs
- Lower values = less budget usage, more cost savings

**Dependencies**: Requires `maximize_budget_utilization: True`

**Example**:
```python
config = {
    'maximize_budget_utilization': True,
    'budget_maximization_weight_multiplier': 2.0  # 2x stronger
}
```

---

#### `min_budget_utilization` (float, default: 0.0)
**Description**: Minimum budget utilization percentage (0.0 to 1.0).

**Range**: 0.0 (0%) to 1.0 (100%)

**Impact**:
- Adds hard constraint: `allocated_cost >= max_budget * min_budget_utilization`
- Forces minimum budget usage
- Guarantees at least X% of budget is used

**Side Effects**:
- May cause infeasibility if budget is too large relative to available capacity
- Works with `budget_flexibility`:
  - If True: applies to total project budget
  - If False: applies to per-month budget
- Higher values = more restrictive, may limit solution space

**Mutual Exclusivity**: None (can combine with `maximize_budget_utilization`)

**Example**:
```python
config = {
    'min_budget_utilization': 0.8  # Must use at least 80% of budget
}
```

---

### 5. Skill Matching

#### `allow_allocation_without_skills` (bool, default: False)
**Description**: Allow employees to be allocated even if they don't have any required skills.

**Impact**:
- Creates allocation variables for employees without matching skills
- Applies `no_skills_penalty_multiplier` to cost
- Increases solution space significantly

**Side Effects**:
- **Cost Impact**: Applies penalty multiplier (default 2x) to allocations without skills
- **Quality Impact**: Lower skill quality scores (0.0)
- **Interacts with skill development**:
  - If both True: skill dev for partial skills, no-skills for complete mismatch
  - If only no-skills True: all mismatches use no-skills allocation
- Allocations are flagged with `no_required_skills: True` in output

**Mutual Exclusivity**: 
- Not mutually exclusive with `allow_skill_development`:
  - Skill dev: employees with partial skills (at least one matching skill)
  - No skills: employees with no matching skills
  - Both can be enabled simultaneously

**Example**:
```python
config = {
    'allow_allocation_without_skills': True,
    'no_skills_penalty_multiplier': 2.0  # 2x cost penalty
}
```

---

#### `no_skills_penalty_multiplier` (float, default: 2.0)
**Description**: Cost multiplier for allocations without required skills.

**Range**: 1.0 (no penalty) to any positive value

**Impact**:
- Multiplies employee cost when allocating without skills
- Higher values = stronger penalty, less likely to allocate without skills
- Applied in objective function: `cost * (multiplier - 1.0)` as additional penalty

**Side Effects**:
- Only active if `allow_allocation_without_skills: True`
- Higher values discourage no-skills allocations
- Lower values (close to 1.0) make no-skills allocations more attractive
- Interacts with `maximize_budget_utilization` - penalty still applies

**Dependencies**: Requires `allow_allocation_without_skills: True`

**Example**:
```python
config = {
    'allow_allocation_without_skills': True,
    'no_skills_penalty_multiplier': 3.0  # 3x cost penalty
}
```

---

### 6. Team Composition

#### `enable_team_diversity` (bool, default: True)
**Description**: Enable team composition diversity constraints and penalties.

**Impact**:
- Adds diversity penalty variables to objective
- Penalizes teams that are too small (single-person teams)
- Encourages balanced team composition

**Side Effects**:
- Only active if `diversity_weight > 0` in weights
- Adds solver variables and constraints (increases complexity)
- May conflict with cost minimization (diversity may cost more)
- Works with `min_grade_diversity` for grade-level diversity

**Dependencies**: Requires `diversity_weight > 0` in weights

**Example**:
```python
config = {'enable_team_diversity': True}
weights = {'diversity_weight': 0.05}
```

---

#### `min_grade_diversity` (bool, default: False)
**Description**: Require multiple grade levels in teams (future implementation).

**Note**: Framework exists but not fully implemented in constraints.

---

### 7. Employee Preferences

#### `enable_employee_preferences` (bool, default: True)
**Description**: Consider employee project preferences in optimization.

**Impact**:
- Applies penalty to allocations for non-preferred projects
- Employees with preferred projects get lower penalty
- Encourages matching employees to preferred projects

**Side Effects**:
- Only active if `preference_weight > 0` in weights
- May conflict with cost minimization (preferred projects may cost more)
- Requires `preferred_projects` column in employees DataFrame

**Dependencies**: Requires `preference_weight > 0` in weights

**Example**:
```python
config = {'enable_employee_preferences': True}
weights = {'preference_weight': 0.05}
```

---

### 8. Role-Based Allocation

#### `enforce_role_allocation` (bool, default: True)
**Description**: Enforce role-based allocation constraints (DEV/QA/BA).

**Impact**:
- Adds hard constraints for minimum role allocations per project-month
- Uses `min_role_allocation` to set minimums
- Uses `role_allocation_ratios` for soft constraints (via objective)

**Side Effects**:
- May cause infeasibility if not enough employees of each role
- Interacts with `min_team_size` - role constraints are additional
- Adds many constraints (one per role per project-month)
- Works with `role_balance_weight` in objective for ratio enforcement

**Dependencies**: 
- Requires `min_role_allocation` dict
- Requires `role_allocation_ratios` dict
- Works with `role_balance_weight` in weights

**Example**:
```python
config = {
    'enforce_role_allocation': True,
    'min_role_allocation': {'DEV': 0.1, 'QA': 0.05, 'BA': 0.0},
    'role_allocation_ratios': {'DEV': 0.50, 'QA': 0.30, 'BA': 0.20}
}
```

---

#### `min_role_allocation` (dict, default: {'DEV': 0.1, 'QA': 0.05, 'BA': 0.0})
**Description**: Minimum FTE allocation per role per project per month.

**Format**: `{'DEV': float, 'QA': float, 'BA': float}`

**Impact**:
- Hard constraint: `sum(role_allocations) >= min_role_allocation[role]`
- Ensures minimum presence of each role
- 0.0 means role is optional

**Side Effects**:
- May cause infeasibility if not enough employees of required role
- Higher values = more restrictive
- Interacts with `enforce_role_allocation` (only active if True)

**Dependencies**: Requires `enforce_role_allocation: True`

**Example**:
```python
config = {
    'enforce_role_allocation': True,
    'min_role_allocation': {'DEV': 0.2, 'QA': 0.1, 'BA': 0.05}  # Higher minimums
}
```

---

#### `role_allocation_ratios` (dict, default: {'DEV': 0.50, 'QA': 0.30, 'BA': 0.20})
**Description**: Target role allocation ratios (soft constraints via objective).

**Format**: `{'DEV': float, 'QA': float, 'BA': float}` (should sum to ~1.0)

**Impact**:
- Soft constraint enforced via `role_balance_weight` in objective
- Encourages maintaining target ratios
- Penalizes deviation from target ratios

**Side Effects**:
- Only active if `role_balance_weight > 0` in weights
- Soft constraint (can be violated if other objectives conflict)
- Higher `role_balance_weight` = stronger enforcement

**Dependencies**: 
- Requires `enforce_role_allocation: True`
- Requires `role_balance_weight > 0` in weights

**Example**:
```python
config = {
    'enforce_role_allocation': True,
    'role_allocation_ratios': {'DEV': 0.60, 'QA': 0.30, 'BA': 0.10}  # More DEV focus
}
weights = {'role_balance_weight': 0.15}  # Stronger enforcement
```

---

## Weight Options

Weights control the relative importance of different optimization objectives. All weights are in the `weights` parameter (not `config`).

### Available Weights

| Weight | Default | Description | Impact |
|--------|---------|------------|--------|
| `cost_weight` | 0.30 | Cost minimization | Higher = prioritize cheaper employees |
| `skill_weight` | 0.15 | Skill quality matching | Higher = prioritize better skill matches |
| `fragmentation_weight` | 0.10 | Reduce small allocations | Higher = prefer larger, fewer allocations |
| `continuity_weight` | 0.10 | Allocation stability | Higher = prefer stable allocations across months |
| `balance_weight` | 0.10 | Workload balancing | Higher = prefer balanced employee utilization |
| `preference_weight` | 0.05 | Employee preferences | Higher = prioritize preferred projects |
| `diversity_weight` | 0.05 | Team diversity | Higher = prefer diverse teams |
| `leveling_weight` | 0.05 | Resource leveling | Higher = prefer smooth workload changes |
| `role_balance_weight` | 0.10 | Role allocation balance | Higher = enforce role ratios more strictly |

**Note**: Weights should typically sum to ~1.0, but the solver will normalize them.

---

## Mutual Exclusivity & Dependencies

### Mutually Exclusive Options

1. **`discrete_allocations`**: 
   - Mutually exclusive with continuous allocations (either True or False)
   - Changes solver type and constraint handling

2. **`budget_flexibility`**:
   - Mutually exclusive approaches:
     - `True`: Total budget constraint (flexible across months)
     - `False`: Per-month budget constraints (strict monthly limits)

### Dependent Options

1. **`skill_dev_max_fte`** → Requires `allow_skill_development: True`
2. **`allocation_increments`** → Requires `discrete_allocations: True`
3. **`budget_maximization_weight_multiplier`** → Requires `maximize_budget_utilization: True`
4. **`no_skills_penalty_multiplier`** → Requires `allow_allocation_without_skills: True`
5. **`min_role_allocation`** → Requires `enforce_role_allocation: True`
6. **`role_allocation_ratios`** → Requires `enforce_role_allocation: True`
7. **`enable_team_diversity`** → Requires `diversity_weight > 0`
8. **`enable_employee_preferences`** → Requires `preference_weight > 0`

### Compatible Options (Can Be Used Together)

- `allow_skill_development` + `allow_allocation_without_skills` (complementary)
- `maximize_budget_utilization` + `min_budget_utilization` (can combine)
- `enforce_role_allocation` + `enable_team_diversity` (different aspects)
- All weight options (multi-objective optimization)

---

## Impact Analysis

### Configuration Impact Matrix

| Config Option | Solver Performance | Solution Quality | Budget Utilization | Cost | Flexibility |
|---------------|-------------------|------------------|-------------------|------|-------------|
| `discrete_allocations: True` | ⬇️ Slower | ⬇️ May be suboptimal | ⬇️ Lower | ➡️ Similar | ⬇️ Less flexible |
| `max_employee_per_project: 0.5` | ➡️ Similar | ⬆️ Better distribution | ⬇️ Lower | ➡️ Similar | ⬇️ Less flexible |
| `min_team_size: 2` | ➡️ Similar | ⬆️ Better teams | ⬇️ May reduce | ➡️ Similar | ⬇️ Less flexible |
| `budget_flexibility: False` | ➡️ Similar | ⬇️ More restrictive | ⬇️ Lower | ➡️ Similar | ⬇️ Less flexible |
| `maximize_budget_utilization: True` | ➡️ Similar | ➡️ Similar | ⬆️ Higher | ⬆️ Higher | ➡️ Similar |
| `allow_allocation_without_skills: True` | ➡️ Similar | ⬇️ Lower quality | ⬆️ Higher | ⬆️ Higher | ⬆️ More flexible |
| `allow_skill_development: True` | ⬇️ Slightly slower | ⬆️ Better learning | ➡️ Similar | ➡️ Similar | ⬆️ More flexible |

---

## Side Effects & Interactions

### 1. Budget Maximization vs. Cost Minimization

**Conflict**: Both are in the objective function with opposite goals.

**Resolution**:
- Default: Cost minimization wins (weight 0.30 vs. budget max 0.03)
- To prioritize budget: Set `budget_maximization_weight_multiplier > 1.0`
- Recommended: `multiplier = 1.5` to 2.0 for strong budget maximization

**Side Effect**: Higher budget utilization = higher costs (uses more resources)

---

### 2. Skill Development vs. No-Skills Allocation

**Relationship**: Complementary, not mutually exclusive.

- **Skill Development**: Employees with partial skills (at least one matching)
- **No-Skills**: Employees with no matching skills

**Impact**:
- Both enabled: Maximum flexibility, can allocate anyone
- Only skill dev: Can only allocate employees with partial skills
- Only no-skills: Can allocate anyone, but all mismatches get penalty

---

### 3. Discrete Allocations Impact

**Side Effects**:
- Changes all constraint coefficients (uses increment values)
- Affects minimum allocation (uses `min(increments)`)
- Affects capacity constraints (uses `max(increments)`)
- May reduce budget utilization (can't use exact amounts)
- Slower solver (integer programming)

**Recommendation**: Use for realistic planning, disable for maximum flexibility

---

### 4. Role Allocation Constraints

**Side Effects**:
- Adds many constraints (one per role per project-month)
- May cause infeasibility if role distribution is insufficient
- Interacts with `min_team_size` (both add constraints)
- Works with `role_balance_weight` for soft ratio enforcement

**Recommendation**: 
- Set `min_role_allocation` to 0.0 for optional roles
- Use `role_allocation_ratios` for soft constraints (more flexible)

---

### 5. Budget Flexibility Impact

**True (Flexible)**:
- Total budget across all months
- Can overspend in some months, underspend in others
- Better utilization, more flexible

**False (Strict)**:
- Per-month budget limits
- Must stay within monthly budget each month
- More restrictive, may reduce utilization

**Recommendation**: Use `True` for better budget utilization

---

## Recommended Configurations

### 1. Cost-Optimized (Default)
```python
config = {
    'maximize_budget_utilization': False,
    'allow_allocation_without_skills': False,
    'budget_flexibility': True
}
```
**Use Case**: Minimize costs while meeting requirements

---

### 2. Budget-Maximized
```python
config = {
    'maximize_budget_utilization': True,
    'budget_maximization_weight_multiplier': 1.5,
    'min_budget_utilization': 0.0,  # Or set to 0.8 for 80% minimum
    'budget_flexibility': True
}
```
**Use Case**: Maximize budget utilization

---

### 3. Flexible Skill Matching
```python
config = {
    'allow_skill_development': True,
    'allow_allocation_without_skills': True,
    'no_skills_penalty_multiplier': 2.0,
    'skill_dev_max_fte': 0.2
}
```
**Use Case**: When skill matching is challenging, allow learning and no-skills allocations

---

### 4. Strict Role Requirements
```python
config = {
    'enforce_role_allocation': True,
    'min_role_allocation': {'DEV': 0.2, 'QA': 0.1, 'BA': 0.05},
    'role_allocation_ratios': {'DEV': 0.50, 'QA': 0.30, 'BA': 0.20}
}
weights = {'role_balance_weight': 0.15}  # Strong enforcement
```
**Use Case**: Projects requiring specific role distributions

---

### 5. Realistic Planning (Discrete)
```python
config = {
    'discrete_allocations': True,
    'allocation_increments': [0.25, 0.5, 0.75, 1.0],
    'max_employee_per_project': 0.8
}
```
**Use Case**: Real-world resource planning with discrete allocations

---

### 6. Maximum Flexibility
```python
config = {
    'allow_skill_development': True,
    'allow_allocation_without_skills': True,
    'budget_flexibility': True,
    'maximize_budget_utilization': True,
    'budget_maximization_weight_multiplier': 1.5,
    'min_team_size': 0,  # No minimum team size
    'enforce_role_allocation': False  # No role constraints
}
```
**Use Case**: When you need maximum allocation flexibility

---

## Troubleshooting

### Solver Returns INFEASIBLE

**Possible Causes**:
1. `min_budget_utilization` too high relative to available capacity
2. `min_role_allocation` too high - not enough employees of required roles
3. `min_team_size` too high - not enough employees
4. Budgets too small for extended timelines
5. `max_employee_per_project` too low with high `min_team_size`

**Solutions**:
- Reduce `min_budget_utilization`
- Set `min_role_allocation` to 0.0 for optional roles
- Reduce `min_team_size`
- Increase project budgets
- Enable `allow_allocation_without_skills`

---

### Low Budget Utilization

**Possible Causes**:
1. `maximize_budget_utilization: False` (cost minimization wins)
2. `budget_maximization_weight_multiplier` too low (< 1.0)
3. Employee capacity constraints
4. `max_employee_per_project` too low

**Solutions**:
- Set `maximize_budget_utilization: True`
- Increase `budget_maximization_weight_multiplier` to 1.5 or higher
- Add more employees or increase capacity
- Increase `max_employee_per_project`

---

### High Costs

**Possible Causes**:
1. `maximize_budget_utilization: True` with high multiplier
2. `allow_allocation_without_skills: True` with penalty
3. Expensive employees being allocated

**Solutions**:
- Reduce `budget_maximization_weight_multiplier`
- Increase `no_skills_penalty_multiplier` to discourage no-skills allocations
- Adjust `cost_weight` in weights to prioritize cost more

---

## Summary Table

| Config | Type | Default | Mutually Exclusive With | Dependencies | Key Impact |
|--------|------|---------|------------------------|--------------|------------|
| `max_employee_per_project` | float | 0.8 | None | None | Limits single-employee dependency |
| `min_team_size` | int | 1 | None | None | Ensures minimum team size |
| `allow_skill_development` | bool | True | None | None | Enables learning allocations |
| `skill_dev_max_fte` | float | 0.2 | None | `allow_skill_development` | Limits skill dev allocation |
| `discrete_allocations` | bool | False | Continuous allocations | `allocation_increments` | Uses discrete increments |
| `allocation_increments` | list | [0.25,0.5,0.75,1.0] | None | `discrete_allocations` | Defines discrete levels |
| `budget_flexibility` | bool | True | Per-month budgets | None | Allows budget borrowing |
| `maximize_budget_utilization` | bool | False | Cost minimization (conflicting) | `budget_maximization_weight_multiplier` | Maximizes budget usage |
| `budget_maximization_weight_multiplier` | float | 1.0 | None | `maximize_budget_utilization` | Controls budget max strength |
| `min_budget_utilization` | float | 0.0 | None | None | Forces minimum budget usage |
| `allow_allocation_without_skills` | bool | False | None | `no_skills_penalty_multiplier` | Allows no-skills allocation |
| `no_skills_penalty_multiplier` | float | 2.0 | None | `allow_allocation_without_skills` | Penalty for no-skills |
| `enable_team_diversity` | bool | True | None | `diversity_weight > 0` | Enforces team diversity |
| `enable_employee_preferences` | bool | True | None | `preference_weight > 0` | Uses preferences |
| `enforce_role_allocation` | bool | True | None | `min_role_allocation`, `role_allocation_ratios` | Enforces role constraints |
| `min_role_allocation` | dict | {DEV:0.1,QA:0.05,BA:0.0} | None | `enforce_role_allocation` | Minimum role FTE |
| `role_allocation_ratios` | dict | {DEV:0.5,QA:0.3,BA:0.2} | None | `enforce_role_allocation` | Target role ratios |

---

## Quick Reference

### Enable Budget Maximization
```python
config = {
    'maximize_budget_utilization': True,
    'budget_maximization_weight_multiplier': 1.5
}
```

### Allow Allocations Without Skills
```python
config = {
    'allow_allocation_without_skills': True,
    'no_skills_penalty_multiplier': 2.0
}
```

### Use Discrete Allocations
```python
config = {
    'discrete_allocations': True,
    'allocation_increments': [0.25, 0.5, 0.75, 1.0]
}
```

### Enforce Role Requirements
```python
config = {
    'enforce_role_allocation': True,
    'min_role_allocation': {'DEV': 0.1, 'QA': 0.05, 'BA': 0.0}
}
```

---

**Last Updated**: Based on `allocate_fully_optimized.py` implementation
**Version**: 1.0

