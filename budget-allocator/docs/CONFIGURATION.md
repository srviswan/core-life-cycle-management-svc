# Budget Allocator - Configuration Guide

## Overview

Configuration is managed through `config.py`. You can modify these settings to adjust allocation behavior.

## Priority Weights

Controls how project priority is calculated:

```python
PRIORITY_WEIGHTS = {
    'driver': 0.4,    # Weight for project driver
    'impact': 0.4,    # Weight for impact level
    'rank': 0.2       # Weight for rank
}
```

**Note**: Weights should sum to 1.0.

### Driver Normalization

Drivers are normalized to 0-1 scale:
- `Regulatory`, `Compliance` → 1.0 (highest)
- `Strategic` → 0.9
- `Product` → 0.7
- `Operational` → 0.5
- `Maintenance` → 0.3
- `Research` → 0.4

### Impact Normalization

Impacts are normalized to 0-1 scale:
- `High`, `Critical` → 1.0
- `Important` → 0.8
- `Medium` → 0.5
- `Low`, `Minor` → 0.0-0.2

### Rank Normalization

Ranks are normalized assuming max rank of 20:
- Rank 1 → 1.0 (highest priority)
- Rank 10 → 0.5
- Rank 20 → 0.0

## Waterfall Multiplier

Controls how strongly higher priority projects are favored:

```python
PRIORITY_WATERFALL_MULTIPLIER = 100.0
```

Higher values create stronger waterfall effect (higher priority projects allocated much more strongly).

Formula:
```
priority_factor = base_weight / (waterfall_multiplier ^ (max_priority - project_priority))
```

Example:
- If multiplier = 100.0
- Project with priority 0.9 gets factor = 1.0 / (100^0.1) ≈ 0.8
- Project with priority 0.5 gets factor = 1.0 / (100^0.5) ≈ 0.01

This ensures high priority projects are allocated first.

## Solver Type

```python
SOLVER_TYPE = 'GLOP'  # or 'CBC'
```

- `GLOP` - Continuous linear programming (faster, for cost-based allocation)
- `CBC` - Mixed integer programming (slower, for discrete allocations)

For cost-based allocation, `GLOP` is recommended.

## Objective Function Weights

Controls preferences in the optimization:

```python
EFFORT_ESTIMATE_WEIGHT = 0.1  # Weight for effort estimate alignment
SKILL_MATCH_WEIGHT = 0.2      # Weight for skill match quality
PRIORITY_WEIGHT = 1.0         # Base weight for priority
```

### Effort Estimate Weight

How much to prefer allocations that align with effort estimates:
- Higher = stronger preference for matching effort estimates
- Lower = less consideration of effort estimates

### Skill Match Weight

How much to prefer better skill matches:
- Higher = strongly prefer resources with better skill matches
- Lower = less consideration of skill match quality

### Priority Weight

Base weight for priority in objective function. Combined with waterfall multiplier.

## Monthly Budget Distribution

```python
MONTHLY_BUDGET_DISTRIBUTION = 'even'
```

- `even` - Distribute budget evenly across project months
- `custom` - Future: allow custom distribution (not yet implemented)

## Skill Matching Threshold

```python
MIN_SKILL_MATCH_SCORE = 0.0
```

Minimum skill match score to consider (0.0 = any match allowed).

Currently not used as mandatory skills are hard constraint.

## Custom Configuration

You can override configuration when calling the allocator:

```python
from budget_allocator import budget_allocator

config = {
    'solver_type': 'GLOP',
    'priority_waterfall_multiplier': 50.0,  # Less aggressive waterfall
    'effort_estimate_weight': 0.2,          # More weight on effort
    'skill_match_weight': 0.3,              # More weight on skills
    'priority_weight': 1.0
}

allocations = budget_allocator(resources_df, projects_df, config=config)
```

## Configuration Examples

### Aggressive Waterfall (High Priority First)

```python
PRIORITY_WATERFALL_MULTIPLIER = 200.0  # Very strong preference
PRIORITY_WEIGHT = 2.0                  # Higher base weight
```

### Balanced Allocation

```python
PRIORITY_WATERFALL_MULTIPLIER = 10.0   # Moderate preference
SKILL_MATCH_WEIGHT = 0.3                # More weight on skills
EFFORT_ESTIMATE_WEIGHT = 0.2            # More weight on effort
```

### Skill-Focused

```python
SKILL_MATCH_WEIGHT = 0.5                # High weight on skills
PRIORITY_WATERFALL_MULTIPLIER = 50.0     # Moderate priority preference
```

## Best Practices

1. **Start with defaults**: Default configuration works well for most cases
2. **Adjust gradually**: Change one parameter at a time
3. **Monitor results**: Check if allocations make business sense
4. **Balance objectives**: Don't over-weight a single objective
5. **Test scenarios**: Try different configurations to find what works best

## Troubleshooting

### Projects not allocated in priority order

- Increase `PRIORITY_WATERFALL_MULTIPLIER`
- Increase `PRIORITY_WEIGHT`

### Poor skill matching

- Increase `SKILL_MATCH_WEIGHT`
- Review mandatory skills (too restrictive?)

### Effort estimates ignored

- Increase `EFFORT_ESTIMATE_WEIGHT`
- Ensure effort estimates are provided in input

### Solver too slow

- Use `GLOP` instead of `CBC`
- Reduce number of projects/resources
- Reduce time horizon (fewer months)
