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

## Team/Sub-team/Pod Alignment (Hard Constraint + Hierarchical Fallback)

### Overview

The system enforces **hard constraints** on team matching to prevent moving people across teams. It uses a hierarchical fallback strategy within the same team.

### Hard Constraint: Team Matching

**Critical Rule**: If a project specifies a team, **ONLY** resources from that team can be allocated. Resources from different teams are **excluded** (hard constraint).

### Hierarchical Fallback Strategy (Within Same Team)

The system uses a hierarchical fallback approach, but **never falls back below the specified level**:

1. **If Pod is specified**:
   - **First**: Try to match resources from the same pod
   - **Fallback**: If no pod match, try sub_team match (within same team)
   - **NOT below**: Never falls back to just team match if sub_team is specified

2. **If Sub_team is specified (but no pod)**:
   - **First**: Try to match resources from the same sub_team
   - **Fallback**: If no sub_team match, try team match (within same team)
   - **NOT below**: Never falls back below team level

3. **If only Team is specified**:
   - Allow any resource from that team

### Alignment Score Calculation

The team alignment score is calculated based on matches:
- **Perfect match** (team + sub_team + pod): 1.0
- **Pod match only**: 1.0
- **Sub_team match (pod fallback)**: 0.8
- **Sub_team match only**: 0.8
- **Team match (sub_team fallback)**: 0.5
- **Team match only**: 0.5
- **No match**: 0.0 (resource excluded if team constraint applies)

### Implementation

- **Hard constraint**: Variables are not created for resource-project pairs where team constraint is violated
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

## Dummy Resources (New Hires)

### Overview

After allocation, if projects have remaining budget that cannot be fulfilled by existing resources (due to team constraints or capacity limits), the system creates **dummy resources** representing new hires needed.

### When Dummy Resources Are Created

Dummy resources are created when:
- Project has remaining budget after allocation (> 1% of monthly budget)
- Existing resources cannot fulfill the budget (due to team constraints, capacity, or availability)
- Project is not an efficiency project (has budget > 0)

### Dummy Resource Characteristics

Each dummy resource represents a new hire with:
- **Location**: Best location (most common location among existing resources, for location balance)
- **Cost**: Average monthly cost of existing resources (for cost balance)
- **Team/Sub-team/Pod**: Inherited from the project's preferred team/sub_team/pod
- **FTE**: Calculated as `remaining_budget / average_monthly_cost`

### Dummy Resource Format

- **Resource ID**: `DUMMY_{project_id}_{month}`
- **Resource Name**: `New Hire - {project_name} - {month}`
- **Explanation**: Includes FTE needed, location, team/sub_team/pod, and cost

### Output

Dummy resources appear in:
- **Allocations sheet**: Marked with `DUMMY_` prefix in resource_id
- **Project Summary**: Count of dummy resources needed per project
- **Monthly View**: Included in monthly allocation views

### Use Case

Dummy resources help identify:
- Where new hires are needed
- What team/sub_team/pod they should join
- What location they should be in (for balance)
- What cost level is appropriate

## Future Enhancements

- Discrete allocation increments (0.25, 0.5, 0.75, 1.0 FTE)
- Project dependencies and sequencing
- Dynamic priority adjustment
- Multi-objective optimization with Pareto frontier
- Sensitivity analysis
