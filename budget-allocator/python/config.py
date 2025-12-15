"""config.py - Configuration and priority weights for budget allocator"""

# Priority calculation weights (must sum to 1.0)
PRIORITY_WEIGHTS = {
    'driver': 0.4,
    'impact': 0.4,
    'rank': 0.2
}

# Waterfall multiplier - higher values create stronger priority preference
PRIORITY_WATERFALL_MULTIPLIER = 100.0

# Solver type: 'GLOP' for continuous, 'CBC' for integer/mixed-integer
SOLVER_TYPE = 'GLOP'

# Skill matching thresholds
MIN_SKILL_MATCH_SCORE = 0.0  # Minimum skill match to consider (0.0 = any match)

# Monthly budget distribution method: 'even' or 'custom'
MONTHLY_BUDGET_DISTRIBUTION = 'even'

# Effort estimate weight in objective function
EFFORT_ESTIMATE_WEIGHT = 0.1

# Skill match weight in objective function
SKILL_MATCH_WEIGHT = 0.2

# Priority weight in objective function
PRIORITY_WEIGHT = 1.0

# Region diversity weight in objective function
# Higher values encourage cross-region allocation (diversity bonus)
REGION_DIVERSITY_WEIGHT = 0.1
