# Shared Configuration Files

## Overview

Both `run_demo.py` and `run_custom_test.py` now use a shared configuration system that ensures consistent behavior across both scripts.

## Configuration Priority

The config loader searches for configuration files in this order:

1. **User-specific config** (highest priority):
   - `allocator_config.json`
   - `allocator_config.yaml`
   - `allocator_config.yml`

2. **Shared config** (fallback):
   - `allocator_config_shared.json`
   - `allocator_config_shared.yaml`
   - `allocator_config_shared.yml`

3. **Inline defaults** (lowest priority):
   - Script-specific defaults if no config files found

## Shared Config Files

### `allocator_config_shared.yaml` / `allocator_config_shared.json`

These files contain optimized settings for **maximum budget utilization and full employee allocation**:

- **Budget Management:**
  - `maximize_budget_utilization: true`
  - `budget_maximization_weight_multiplier: 50.0` (very high)
  - `min_budget_utilization: 0.0`

- **Team Constraints:**
  - `max_employee_per_project: 1.0` (allows full allocation)
  - `min_team_size: 0` (no minimum requirement)
  - `enforce_role_allocation: false` (flexible allocation)

- **Weights:**
  - All weights set to `0.0` to prioritize budget maximization
  - No workload balancing, skill penalties, or other constraints

## Usage

### Using Shared Config (Default)

Simply run either script without specifying a config file:

```bash
python3 run_demo.py
python3 run_custom_test.py
```

Both will automatically use `allocator_config_shared.yaml` if no user config exists.

### Using Your Own Config

Create your own `allocator_config.yaml` or `allocator_config.json` in the same directory. It will take precedence over the shared config:

```bash
python3 run_demo.py allocator_config.yaml
python3 run_custom_test.py allocator_config.yaml
```

### Overriding with Command Line

You can also specify a different config file:

```bash
python3 run_demo.py /path/to/custom_config.yaml
python3 run_custom_test.py /path/to/custom_config.yaml
```

## Behavior Consistency

Both scripts now behave consistently:

- **With shared config:** Both prioritize budget utilization and full employee allocation
- **With user config:** Both use the same user-defined settings
- **Without config:** Both use the same inline defaults (matching shared config)

## Customizing the Shared Config

To customize the shared config for your needs:

1. Copy `allocator_config_shared.yaml` to `allocator_config.yaml`
2. Modify the settings as needed
3. Your custom config will be used automatically

Or modify `allocator_config_shared.yaml` directly if you want to change the default behavior for all users.
