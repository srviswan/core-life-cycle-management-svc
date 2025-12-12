# Driver Caps, Funding Source Prioritization, and Skill Operators Guide

## Overview

The budget allocator supports three important enhancements:
1. **Driver-based allocation caps** - Projects within a driver are fully allocated before moving to other drivers
2. **Funding source prioritization** - Priority and rank are considered within each funding_source
3. **Simplified skill system with regex** - Easy-to-use skill matching with AND/OR operators and regex patterns

**Note**: For detailed information on the simplified skill system, see [SKILL_SYSTEM.md](SKILL_SYSTEM.md).

## Funding Source Prioritization

### How It Works

Projects are prioritized **within each funding_source** based on their calculated priority and rank.

**Allocation Logic:**
- Funding sources are sorted by their maximum priority (highest priority project in that source)
- Within each funding_source, projects are ranked by priority (highest first)
- Higher priority funding sources are allocated first
- Within each funding_source, higher priority/rank projects are allocated first

### Example

**Funding Source: Internal**
- Project A: priority=0.8, rank=1
- Project B: priority=0.6, rank=2
- **Funding source priority = 0.8** (max of projects in this source)

**Funding Source: Grant**
- Project C: priority=0.9, rank=1
- Project D: priority=0.7, rank=2
- **Funding source priority = 0.9** (max of projects in this source)

**Allocation Order:**
1. Grant projects (funding_source priority 0.9 > 0.8)
   - Project C (priority 0.9, rank 1) - allocated first
   - Project D (priority 0.7, rank 2) - allocated second
2. Internal projects (funding_source priority 0.8)
   - Project A (priority 0.8, rank 1) - allocated first
   - Project B (priority 0.6, rank 2) - allocated second

### Implementation

- Funding source priority = maximum priority of all projects in that funding_source
- Within funding_source, projects are ranked by priority (1 = highest)
- The objective function applies funding_source waterfall multiplier
- Rank within funding_source also affects allocation (lower rank = higher priority)

### Priority Hierarchy

The allocation priority follows this hierarchy (strongest to weakest):
1. **Driver** (driver_waterfall_multiplier × 10.0)
2. **Funding Source** (funding_source_waterfall_multiplier × 5.0)
3. **Rank within Funding Source** (waterfall_multiplier)
4. **Project Priority** (waterfall_multiplier)

This ensures:
- Driver caps are respected first
- Funding sources are prioritized
- Within funding_source, rank matters
- Overall project priority is considered

## Driver-Based Allocation Caps

### How It Works

For each **driver**, the system calculates the total allocated budget (sum of `alloc_budget` for all projects in that driver). This becomes the "cap" for that driver.

**Allocation Logic:**
- Projects are grouped by driver
- Drivers are sorted by priority (higher priority drivers allocated first)
- Within each driver, projects are allocated fully (up to their budgets) before moving to the next driver
- This creates a **waterfall effect within drivers**

### Example

**Driver: Regulatory**
- Project A: alloc_budget = 100,000
- Project B: alloc_budget = 50,000
- **Driver cap = 150,000**

**Driver: Product**
- Project C: alloc_budget = 75,000
- **Driver cap = 75,000**

**Allocation Order:**
1. Allocate to Regulatory projects (A and B) up to 150,000 total
2. Only after Regulatory is fully allocated, allocate to Product projects

### Implementation

- Driver priority is calculated from the `driver` field using `normalize_driver()`
- Higher priority drivers (e.g., Regulatory=1.0) get allocated before lower priority (e.g., Product=0.7)
- The objective function uses a very high multiplier (`driver_waterfall_multiplier = waterfall_multiplier * 10.0`) to ensure driver caps are respected

### Configuration

No additional configuration needed. The system automatically:
- Calculates driver caps from project `alloc_budget` values
- Sorts drivers by priority
- Applies waterfall allocation within drivers

## Skill Operators (AND/OR)

### Overview

Skills can now be marked as:
- **AND (&)**: Resource MUST have ALL specified skills
- **OR (||)**: Resource MUST have AT LEAST ONE of the specified skills

### Skill Format

The `required_skills` field in projects supports the following structure:

```json
{
  "mandatory": ["python"],              // ALL required (AND logic, legacy - converted to mandatory_and)
  "mandatory_and": ["python", "sql"],  // ALL mandatory skills required (AND)
  "mandatory_or": ["java", "c++"],     // AT LEAST ONE mandatory skill required (OR)
  "technical_and": ["python", "sql"],   // ALL technical skills required (AND)
  "technical_or": ["java", "c++"],     // AT LEAST ONE technical skill required (OR)
  "functional_and": ["pricing"],        // ALL functional skills required (AND)
  "functional_or": ["risk", "trading"]  // AT LEAST ONE functional skill required (OR)
}
```

### Logic Rules

**Hard Constraint (Must Pass):**
- ALL `mandatory_and` skills must be present (or legacy `mandatory` - converted to `mandatory_and`)
- AT LEAST ONE `mandatory_or` skill must be present (if specified)
- ALL `technical_and` skills must be present
- AT LEAST ONE `technical_or` skill must be present (if specified)
- ALL `functional_and` skills must be present
- AT LEAST ONE `functional_or` skill must be present (if specified)

**All conditions must be met** for a resource to be allocated to a project.

### Examples

#### Example 1: AND Only

```json
{
  "technical_and": ["python", "sql", "pandas"],
  "functional_and": ["pricing", "risk"]
}
```

**Result:** Resource must have ALL of: python, sql, pandas, pricing, risk

#### Example 2: OR Only

```json
{
  "technical_or": ["java", "python", "c++"],
  "functional_or": ["trading", "risk"]
}
```

**Result:** Resource must have AT LEAST ONE technical skill (java OR python OR c++) AND AT LEAST ONE functional skill (trading OR risk)

#### Example 3: Mixed AND/OR with Mandatory

```json
{
  "mandatory_and": ["python", "sql"],      // Must have BOTH mandatory
  "mandatory_or": ["java", "c++"],         // Must have AT LEAST ONE mandatory
  "technical_and": ["pandas"],             // Must have this technical
  "technical_or": ["numpy", "scipy"],      // Must have AT LEAST ONE technical
  "functional_and": ["pricing"],           // Must have this functional
  "functional_or": ["risk", "trading"]     // Must have AT LEAST ONE functional
}
```

**Result:** Resource must have:
- python AND sql (both mandatory required)
- (java OR c++) - at least one mandatory
- pandas (required technical)
- (numpy OR scipy) - at least one technical
- pricing (required functional)
- (risk OR trading) - at least one functional

#### Example 4: Legacy Format (Backward Compatible)

```json
{
  "technical": ["python", "sql"],
  "functional": ["pricing"],
  "mandatory": ["python"]
}
```

**Result:** Treated as AND logic:
- `technical` → converted to `technical_and` (all required)
- `functional` → converted to `functional_and` (all required)
- `mandatory` → converted to `mandatory_and` (all required)

### Skill Match Score

The skill match score calculation now considers AND/OR logic:

- **AND skills**: Score = matched_skills / total_required_skills
- **OR skills**: Score = 1.0 if at least one matched, 0.0 if none matched
- **Overall score**: Combined AND and OR scores (both must be satisfied)

## Usage in Excel

### Projects Sheet

Add `required_skills` as a JSON string:

```excel
required_skills
{"technical_and":["python","sql"],"technical_or":["java"],"functional_and":["pricing"],"mandatory":["python"]}
```

### Example Project Row

| project_id | project_name | driver | alloc_budget | required_skills |
|------------|--------------|--------|--------------|-----------------|
| 1 | Regulatory Project | Regulatory | 100000 | `{"technical_and":["python","sql"],"mandatory":["python"]}` |
| 2 | Product Project | Product | 50000 | `{"technical_or":["java","python"],"functional_and":["pricing"]}` |

## Driver Priority

Drivers are automatically prioritized based on their type:

| Driver | Priority Score |
|--------|----------------|
| Regulatory, Compliance | 1.0 (highest) |
| Strategic | 0.9 |
| Product | 0.7 |
| Operational | 0.5 |
| Maintenance | 0.3 |
| Research | 0.4 |

Higher priority drivers are allocated first.

## Best Practices

1. **Driver Organization:**
   - Group related projects under the same driver
   - Set realistic `alloc_budget` values per project
   - The system will automatically calculate driver caps

2. **Skill Requirements:**
   - Use `mandatory` or `*_and` for truly required skills
   - Use `*_or` for flexible skill requirements
   - Be specific: too many AND requirements may prevent allocation
   - Use OR for alternative skills (e.g., "java OR python")

3. **Testing:**
   - Test with sample data to verify driver allocation order
   - Verify skill matching with AND/OR combinations
   - Check allocation explanations to understand why resources were allocated

## Troubleshooting

### Projects not allocated in driver order

- Check driver values are consistent (case-insensitive)
- Verify driver priorities are correct
- Review allocation explanations

### Resources not matching skill requirements

- Check AND requirements are not too restrictive
- Verify OR requirements have at least one matching skill
- Review skill match details in allocation explanations

### Driver caps not respected

- Ensure `alloc_budget` values are set correctly
- Check that projects have the same driver value
- Verify driver priority calculation
