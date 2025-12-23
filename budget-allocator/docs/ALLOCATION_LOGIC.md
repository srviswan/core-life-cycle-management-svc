# Budget Allocator - Allocation Logic

## Algorithm Overview

The Budget Allocator uses **Linear Programming (LP)** with OR-Tools to solve the resource allocation problem optimally.

## Problem Formulation

### Decision Variables

For each valid (resource, project, month) combination:
```
x[resource_id, project_id, month] = cost allocated
```

**Validity conditions**:
- Resource has ALL mandatory skills for project (hard constraint)
- Month is within project start_date and end_date

### Constraints

1. **Resource Capacity**
   ```
   Sum of allocations per resource-month ≤ resource monthly cost
   ```
   A resource cannot be allocated more than their monthly cost in any month.

2. **Budget Constraint** (for projects with budget > 0)
   ```
   Sum of allocations per project-month ≤ monthly budget
   ```
   Monthly budget = total budget / number of months

3. **Efficiency Projects** (budget = 0)
   - No budget constraint
   - Can use unallocated resources up to resource capacity

### Objective Function

Maximize:
```
Sum of (priority_factor × allocated_cost) + 
      (skill_match_weight × skill_score × allocated_cost) + 
      (effort_weight × effort_alignment × allocated_cost)
```

Where:
- `priority_factor` = waterfall multiplier based on project priority
- `skill_score` = skill match quality (0.0 to 1.0)
- `effort_alignment` = how well allocation matches effort estimate

## Priority Calculation

### Weighted Sum Formula

```
priority = driver_weight × driver_score + 
           impact_weight × impact_score + 
           rank_weight × rank_score
```

### Normalization

**Driver** (0-1 scale):
- Regulatory, Compliance → 1.0
- Strategic → 0.9
- Product → 0.7
- Operational → 0.5
- Maintenance → 0.3

**Impact** (0-1 scale):
- High, Critical → 1.0
- Important → 0.8
- Medium → 0.5
- Low, Minor → 0.0-0.2

**Rank** (0-1 scale, inverted):
- Rank 1 → 1.0
- Rank 10 → 0.5
- Rank 20 → 0.0

## Waterfall Allocation

Waterfall ensures higher priority projects are allocated first.

### Priority Factor Calculation

```
priority_factor = base_weight / (waterfall_multiplier ^ (max_priority - project_priority))
```

Example with multiplier = 100.0:
- Priority 0.9: factor ≈ 0.8
- Priority 0.7: factor ≈ 0.1
- Priority 0.5: factor ≈ 0.01

This creates exponential preference for higher priority.

## Team/Sub-team/Pod Alignment (Primary Preference)

### Overview

Before skill matching, the system strongly prefers resources that match the project's preferred team, sub-team, and pod. This ensures resources are allocated within their organizational units when possible.

### Alignment Score Calculation

The team alignment score is calculated based on matches:
- **Perfect match** (team + sub_team + pod): 1.0
- **Pod + sub_team match**: 0.8
- **Pod match only**: 0.5
- **Sub_team match only**: 0.3
- **Team match only**: 0.2
- **No match**: 0.0 (fallback to skill-based matching)

### Allocation Strategy

1. **First Priority**: Resources matching project's team/sub_team/pod are strongly preferred
   - Weight in objective function: `TEAM_ALIGNMENT_WEIGHT = 5.0` (much higher than skill matching)
   - This ensures team alignment is prioritized over skill matching

2. **Fallback**: If no resources match team/sub_team/pod preferences:
   - System falls back to skill-based matching
   - Skill matching weight: `SKILL_MATCH_WEIGHT = 0.2` (lower than team alignment)

3. **No Preferences**: If project has no team/sub_team/pod preferences:
   - System uses skill-based matching (neutral behavior)

### Implementation

- Team alignment is calculated **before** skill matching
- Team alignment coefficient in objective function: `TEAM_ALIGNMENT_WEIGHT × team_alignment_score`
- Skill matching coefficient: `SKILL_MATCH_WEIGHT × skill_score`
- Since `TEAM_ALIGNMENT_WEIGHT (5.0) >> SKILL_MATCH_WEIGHT (0.2)`, team-aligned resources are strongly preferred

## Skill Matching

### Mandatory Skills (Hard Constraint)

Resources MUST have ALL mandatory skills to be allocated to a project.

**Implementation**: Variables are not created for resource-project pairs where mandatory skills don't match.

### Skill Match Score (Soft Preference - Fallback)

Calculated for technical and functional skills:

```
technical_score = matched_technical_skills / total_required_technical_skills
functional_score = matched_functional_skills / total_required_functional_skills
overall_score = 0.6 × technical_score + 0.4 × functional_score
```

Used in objective function as fallback when no team alignment is found, or as secondary preference when team alignment exists.

## Effort Estimate Integration

Effort estimates guide allocation to match planned effort.

### Alignment Calculation

```
allocated_man_months = allocated_cost / resource_monthly_cost
target_per_month = effort_estimate / project_months
ratio = allocated_man_months / target_per_month
alignment = exp(-abs(ratio - 1.0))
```

Higher alignment = better match to effort estimate.

Used as soft constraint in objective function.

## Efficiency Projects

Projects with `alloc_budget = 0` are efficiency projects.

**Characteristics**:
- No budget constraint
- Use unallocated resources
- Help measure resource utilization

**Allocation logic**:
- After budgeted projects are allocated, remaining resource capacity can be allocated to efficiency projects
- Objective function still considers priority and skill match

## Solver

### GLOP (Recommended)

- Continuous linear programming
- Fast for cost-based allocation
- Optimal solutions

### CBC (Alternative)

- Mixed integer programming
- Slower
- Use if discrete allocations needed (not currently implemented)

## Solution Extraction

After solver completes:

1. Extract non-zero allocations
2. Calculate skill match scores
3. Calculate effort alignment
4. Generate explanations
5. Aggregate into summary sheets

## Explanation Generation

Each allocation includes an explanation covering:

- **Priority**: Project priority score and rank
- **Skills**: Skill match quality and mandatory skills status
- **Effort**: Effort estimate alignment
- **Efficiency**: Whether it's an efficiency project
- **Waterfall**: Whether it's a waterfall allocation

Example:
> "High priority project (priority: 0.85, rank: #2), perfect skill match (skill score: 95%), effort aligned, waterfall allocation - higher priority than alternatives."

## Performance Considerations

### Complexity

- Variables: O(resources × projects × months)
- Constraints: O(resources × months + projects × months)
- Typical solve time: < 1 second for 61 resources, 17 projects, 12 months

### Optimization Tips

1. **Reduce time horizon**: Fewer months = fewer variables
2. **Filter resources**: Only include relevant resources
3. **Filter projects**: Only include active projects
4. **Use GLOP**: Faster than CBC for continuous variables

## Limitations

1. **Linear programming**: Assumes linear relationships
2. **Continuous allocations**: Cost allocations are continuous (not discrete FTE)
3. **Static priorities**: Priorities don't change during allocation
4. **No dependencies**: Projects are independent (no sequencing)

## Future Enhancements

- Discrete allocation increments (0.25, 0.5, 0.75, 1.0 FTE)
- Project dependencies and sequencing
- Dynamic priority adjustment
- Multi-objective optimization with Pareto frontier
- Sensitivity analysis
