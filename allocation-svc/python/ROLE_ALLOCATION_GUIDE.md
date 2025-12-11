# Role Allocation Ratios Guide

## Overview

The allocator supports role-based allocation ratios to ensure balanced team composition. This guide explains how to configure and use role allocation ratios effectively.

## Configuration Options

### 1. `enforce_role_allocation` (Hard Constraints)

- **Type:** Boolean
- **Default:** `True`
- **When `True`:**
  - Enforces minimum role allocations via hard constraints
  - Can cause solver infeasibility if ratios cannot be met
  - Use when role ratios are mandatory requirements

- **When `False`:**
  - No hard constraints on role allocation
  - Allows more flexible allocation
  - Use when role ratios are preferred but not required

### 2. `role_allocation_ratios` (Target Ratios)

- **Type:** Dictionary
- **Default:** `{'DEV': 0.50, 'QA': 0.30, 'BA': 0.20}`
- **Description:** Target proportions for each role in project allocations
- **Example:**
  ```yaml
  role_allocation_ratios:
    DEV: 0.50  # 50% of allocation should be developers
    QA: 0.30   # 30% should be QA engineers
    BA: 0.20   # 20% should be business analysts
  ```
- **Note:** Ratios should sum to approximately 1.0 (100%)

### 3. `min_role_allocation` (Minimum Requirements)

- **Type:** Dictionary
- **Default:** `{'DEV': 0.1, 'QA': 0.05, 'BA': 0.0}`
- **Description:** Minimum FTE per role per project-month
- **Only applies when:** `enforce_role_allocation: true`
- **Example:**
  ```yaml
  min_role_allocation:
    DEV: 0.1   # At least 0.1 FTE of developers required
    QA: 0.05   # At least 0.05 FTE of QA required
    BA: 0.0    # BA is optional (0 means not required)
  ```

### 4. `role_balance_weight` (Soft Encouragement)

- **Type:** Float (0.0 to 1.0)
- **Default:** `0.10` (when using standard weights)
- **Description:** Weight for encouraging role allocation ratios via penalty in objective function
- **How it works:**
  - Adds a penalty when actual role allocation deviates from target ratios
  - Higher weight = stronger preference for meeting ratios
  - Works independently of `enforce_role_allocation`
  - Soft constraint - won't cause infeasibility

## Recommended Configurations

### Configuration 1: Soft Encouragement (Recommended)

**Use when:** You want to encourage role ratios but allow flexibility

```yaml
enforce_role_allocation: false  # No hard constraints
role_allocation_ratios:
  DEV: 0.50
  QA: 0.30
  BA: 0.20
weights:
  role_balance_weight: 0.05  # Soft encouragement
```

**Benefits:**
- Won't cause solver infeasibility
- Encourages balanced teams when possible
- Flexible allocation when ratios can't be met
- Works well with budget maximization

### Configuration 2: Hard Constraints

**Use when:** Role ratios are mandatory requirements

```yaml
enforce_role_allocation: true  # Hard constraints
min_role_allocation:
  DEV: 0.1
  QA: 0.05
  BA: 0.0
role_allocation_ratios:
  DEV: 0.50
  QA: 0.30
  BA: 0.20
weights:
  role_balance_weight: 0.10  # Additional soft encouragement
```

**Benefits:**
- Guarantees minimum role requirements
- Strong preference for target ratios
- Use when compliance is critical

**Risks:**
- May cause solver infeasibility if requirements can't be met
- Less flexible allocation

### Configuration 3: No Role Constraints

**Use when:** Role composition doesn't matter

```yaml
enforce_role_allocation: false
weights:
  role_balance_weight: 0.0  # Disabled
```

**Benefits:**
- Maximum flexibility
- Focuses on other objectives (budget, skills, etc.)

## How It Works

### Hard Constraints (when `enforce_role_allocation: true`)

1. **Minimum Role Allocation:**
   - For each project-month, ensures minimum FTE per role
   - Example: At least 0.1 FTE of DEV must be allocated

2. **Proportional Allocation:**
   - Tries to maintain target ratios via constraints
   - Implemented as soft constraints in objective function

### Soft Encouragement (when `role_balance_weight > 0`)

1. **Deviation Penalty:**
   - Calculates deviation from target ratio for each role
   - Adds penalty to objective function proportional to deviation
   - Higher weight = stronger preference

2. **Formula:**
   ```
   penalty = role_balance_weight * avg_cost * deviation
   where deviation = max(0, target_ratio - actual_ratio)
   ```

## Best Practices

1. **Start with Soft Constraints:**
   - Use `enforce_role_allocation: false` and `role_balance_weight: 0.05`
   - Monitor if ratios are being met
   - Increase weight if needed

2. **Adjust Weight Based on Priority:**
   - Low weight (0.01-0.05): Gentle preference
   - Medium weight (0.05-0.10): Moderate preference
   - High weight (0.10-0.20): Strong preference

3. **Balance with Other Objectives:**
   - If budget maximization is priority, use lower `role_balance_weight`
   - If team composition is critical, use higher weight

4. **Monitor Results:**
   - Check actual role allocations in output
   - Compare with target ratios
   - Adjust configuration as needed

## Example: Shared Config

The shared config (`allocator_config_shared.yaml`) uses soft encouragement:

```yaml
enforce_role_allocation: false  # No hard constraints
role_allocation_ratios:
  DEV: 0.50
  QA: 0.30
  BA: 0.20
weights:
  role_balance_weight: 0.05  # Encourages ratios when possible
```

This ensures:
- ✅ Role ratios are encouraged when possible
- ✅ Won't cause infeasibility
- ✅ Works well with budget maximization
- ✅ Flexible allocation when ratios can't be met
