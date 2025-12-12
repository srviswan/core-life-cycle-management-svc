# Waterfall Allocation Guide

## Overview

Waterfall allocation ensures that high-priority projects are allocated resources **before** lower-priority projects. This is useful when you have critical projects that must be fully staffed before allocating resources to less critical ones.

## How It Works

When `waterfall_allocation: true` is enabled, the allocator applies a **priority multiplier** to the objective function, making high-priority projects significantly more attractive to allocate resources to.

### Priority Levels

Projects are assigned priority based on their `impact` field:
- **Blocker impact** → Priority 4 (highest - critical/urgent)
- **High impact** → Priority 3
- **Medium impact** → Priority 2
- **Low impact** → Priority 1 (lowest)

If `impact` is not specified, projects default to Priority 2 (Medium).

### Priority Factor Calculation

**Normal mode** (waterfall_allocation: false):
```
priority_factor = 1.0 / priority
- Priority 4 (blocker): factor = 0.25
- Priority 3 (high): factor = 0.33
- Priority 2 (medium): factor = 0.50
- Priority 1 (low): factor = 1.00
```

**Waterfall mode** (waterfall_allocation: true):
```
priority_factor = (1.0 / priority) / priority_waterfall_multiplier
- Priority 4 (blocker): factor = 0.25 / 100 = 0.0025
- Priority 3 (high): factor = 0.33 / 100 = 0.0033
- Priority 2 (medium): factor = 0.50 / 100 = 0.0050
- Priority 1 (low): factor = 1.00 / 100 = 0.0100
```

This means blocker and high-priority projects get **100x stronger preference** (or whatever multiplier you set) in the objective function.

## Configuration

### Basic Configuration

```yaml
waterfall_allocation: true
priority_waterfall_multiplier: 100.0
```

### Advanced Configuration

```yaml
waterfall_allocation: true
priority_waterfall_multiplier: 200.0  # Even stronger preference
waterfall_min_allocation_threshold: 0.5  # Future: minimum allocation % before lower priority
```

### Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `waterfall_allocation` | boolean | `false` | Enable waterfall allocation logic |
| `priority_waterfall_multiplier` | float | `100.0` | Multiplier for priority preference (higher = stronger) |
| `waterfall_min_allocation_threshold` | float | `0.0` | Minimum allocation % for higher priority before lower priority (0.0-1.0) |

## Examples

### Example 1: Enable Waterfall Allocation

```yaml
# allocator_config.yaml
waterfall_allocation: true
priority_waterfall_multiplier: 100.0
```

**Result:**
- Blocker priority projects (Priority 4) get allocated first
- High priority projects (Priority 3) get allocated next
- Medium priority projects (Priority 2) get allocated next
- Low priority projects (Priority 1) get allocated last
- Within each priority level, other factors (skills, cost, etc.) still apply

### Example 2: Strong Waterfall (Very High Multiplier)

```yaml
waterfall_allocation: true
priority_waterfall_multiplier: 1000.0  # Very strong preference
```

**Result:**
- Even stronger preference for high priority projects
- Lower priority projects will only be allocated if high priority projects are fully allocated or infeasible

### Example 3: Combined with Budget Maximization

```yaml
waterfall_allocation: true
priority_waterfall_multiplier: 100.0
maximize_budget_utilization: true
budget_maximization_weight_multiplier: 50.0
```

**Result:**
- High priority projects are allocated first (waterfall)
- Within each priority level, budget maximization is prioritized
- Ensures critical projects get resources before maximizing overall budget

## How Priority is Determined

Priority is automatically determined from the project's `impact` field in the input data:

```python
priority = {
    'blocker': 4,
    'high': 3,
    'medium': 2,
    'low': 1
}.get(impact.lower(), 2)  # Defaults to 2 (medium)
```

If your projects don't have an `impact` field, they will default to Priority 2 (Medium).

## Implementation Details

### Objective Function Impact

In waterfall mode, the priority factor is applied to:
1. **Cost minimization** (when budget maximization is disabled)
2. **Budget maximization** (when enabled)
3. **Skill quality** penalties

This ensures that priority affects all aspects of the allocation decision.

### Soft vs Hard Constraints

Currently, waterfall allocation uses **soft constraints** via priority multipliers:
- High priority projects are strongly preferred
- Lower priority projects can still be allocated if needed
- Won't cause solver infeasibility

Future enhancement: `waterfall_min_allocation_threshold` can be used to add hard constraints that prevent lower priority allocation until higher priority projects meet minimum thresholds.

## Best Practices

1. **Use Appropriate Multiplier:**
   - `50.0-100.0`: Moderate waterfall effect
   - `100.0-200.0`: Strong waterfall effect (recommended)
   - `200.0+`: Very strong waterfall (may over-constrain)

2. **Combine with Budget Maximization:**
   - Waterfall ensures priority order
   - Budget maximization ensures full utilization within each priority level

3. **Monitor Results:**
   - Check if high priority projects are getting allocated first
   - Verify lower priority projects are still getting some allocation
   - Adjust multiplier if needed

4. **Project Priority Assignment:**
   - Ensure projects have appropriate `impact` values
   - Use "blocker" for critical/urgent projects that must be allocated first
   - Use "high" for important projects
   - Use "medium" for normal projects
   - Use "low" for optional/nice-to-have projects

## Example Output

With waterfall allocation enabled, you should see:
- Blocker priority projects: Fully allocated first (highest priority)
- High priority projects: Fully allocated or near-full
- Medium priority projects: Allocated with remaining resources
- Low priority projects: Allocated only if resources remain

## Troubleshooting

**Issue:** Lower priority projects not getting allocated at all
- **Solution:** Reduce `priority_waterfall_multiplier` (try 50.0 instead of 100.0)

**Issue:** Priority not being respected
- **Solution:** Check that projects have `impact` field set correctly
- Verify `waterfall_allocation: true` in config

**Issue:** Solver infeasibility
- **Solution:** Waterfall uses soft constraints, so this shouldn't happen. Check other constraints (budgets, capacity, etc.)
