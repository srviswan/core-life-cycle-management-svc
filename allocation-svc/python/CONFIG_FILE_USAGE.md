# Configuration File Usage

The allocator now supports loading configuration from external JSON or YAML files, making it easier to manage different allocation strategies without modifying code.

## Quick Start

1. **Copy the example config file:**
   ```bash
   cp allocator_config.json.example allocator_config.json
   # OR
   cp allocator_config.yaml.example allocator_config.yaml
   ```

2. **Edit the config file** with your desired settings

3. **Run the allocator** - it will automatically find and load the config file:
   ```bash
   python run_demo.py
   python run_custom_test.py
   python run_demo_with_db.py
   ```

4. **Or specify a custom config file path:**
   ```bash
   python run_demo.py /path/to/my_config.json
   python run_custom_test.py configs/production.yaml
   ```

## Config File Locations

The config loader searches for config files in this order:

1. **Command line argument** (if provided)
2. **Current directory**: `allocator_config.json` or `allocator_config.yaml`
3. **Script directory**: `python/allocator_config.json` or `python/allocator_config.yaml`
4. **Config subdirectory**: `python/config/allocator_config.json` or `python/config/allocator_config.yaml`
5. **Defaults**: If no config file is found, uses default values (empty config dict)

## Configuration Format

### JSON Format

```json
{
  "maximize_budget_utilization": true,
  "budget_maximization_weight_multiplier": 1.5,
  "allow_allocation_without_skills": true,
  "no_skills_penalty_multiplier": 2.0,
  "min_team_size": 1,
  "max_employee_per_project": 0.8,
  "enforce_role_allocation": true,
  "min_role_allocation": {
    "DEV": 0.1,
    "QA": 0.05,
    "BA": 0.0
  }
}
```

### YAML Format

```yaml
maximize_budget_utilization: true
budget_maximization_weight_multiplier: 1.5
allow_allocation_without_skills: true
no_skills_penalty_multiplier: 2.0
min_team_size: 1
max_employee_per_project: 0.8
enforce_role_allocation: true
min_role_allocation:
  DEV: 0.1
  QA: 0.05
  BA: 0.0
```

## Available Configuration Options

See `CONFIGURATION_GUIDE.md` for complete documentation of all configuration options.

### Common Options:

- **Budget Management:**
  - `maximize_budget_utilization` (bool)
  - `budget_maximization_weight_multiplier` (float)
  - `min_budget_utilization` (float, 0.0-1.0)

- **Skill Matching:**
  - `allow_allocation_without_skills` (bool)
  - `no_skills_penalty_multiplier` (float)
  - `allow_skill_development` (bool)
  - `skill_dev_max_fte` (float, 0.0-1.0)

- **Team Constraints:**
  - `max_employee_per_project` (float, 0.0-1.0)
  - `min_team_size` (int)

- **Role Allocation:**
  - `enforce_role_allocation` (bool)
  - `min_role_allocation` (dict)
  - `role_allocation_ratios` (dict)

- **Allocation Type:**
  - `discrete_allocations` (bool)
  - `allocation_increments` (list)

## Programmatic Usage

You can also use the config loader in your own scripts:

```python
from config_loader import get_config, load_config

# Load with automatic file discovery
config = get_config()

# Load from specific file
config = get_config('/path/to/config.json')

# Load without validation
config = load_config('config.json')

# Validate manually
from config_loader import validate_config
validate_config(config)
```

## Validation

The config loader automatically validates configuration values:

- **Type checking**: Ensures booleans are booleans, numbers are numbers, etc.
- **Range checking**: Validates numeric ranges (e.g., `max_employee_per_project` must be 0-1)
- **Structure checking**: Validates dictionary structures (e.g., `role_allocation_ratios` must sum to 1.0)

Invalid configurations will raise `ValueError` with a descriptive error message.

## Example Configurations

### Cost-Minimized Allocation
```json
{
  "maximize_budget_utilization": false,
  "allow_allocation_without_skills": false,
  "budget_flexibility": true
}
```

### Budget-Maximized Allocation
```json
{
  "maximize_budget_utilization": true,
  "budget_maximization_weight_multiplier": 1.5,
  "min_budget_utilization": 0.0,
  "budget_flexibility": true
}
```

### Flexible Skill Matching
```json
{
  "allow_skill_development": true,
  "allow_allocation_without_skills": true,
  "no_skills_penalty_multiplier": 2.0,
  "skill_dev_max_fte": 0.2
}
```

### Role-Based Allocation
```json
{
  "enforce_role_allocation": true,
  "min_role_allocation": {
    "DEV": 0.2,
    "QA": 0.1,
    "BA": 0.05
  },
  "role_allocation_ratios": {
    "DEV": 0.50,
    "QA": 0.30,
    "BA": 0.20
  }
}
```

## YAML Support

To use YAML config files, install PyYAML:

```bash
pip install pyyaml
```

YAML files are more readable for complex configurations with nested structures.

## Troubleshooting

**Config file not found:**
- Check that the file exists in one of the search locations
- Use absolute path: `python run_demo.py /full/path/to/config.json`

**Validation errors:**
- Check that numeric values are within valid ranges
- Ensure boolean values are `true`/`false` (JSON) or `true`/`false` (YAML)
- Verify dictionary structures match expected format

**YAML import error:**
- Install PyYAML: `pip install pyyaml`
- Or use JSON format instead

## Backward Compatibility

All scripts remain backward compatible:
- If no config file is found, defaults are used (empty config dict)
- Inline config dictionaries in code still work
- Config files are optional - not required for basic usage
