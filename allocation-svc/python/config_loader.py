"""config_loader.py - Load allocator configuration from external files

Supports JSON and YAML configuration files.
"""
import json
import os
from pathlib import Path
from typing import Dict, Any, Optional

try:
    import yaml
    YAML_AVAILABLE = True
except ImportError:
    YAML_AVAILABLE = False


def load_config(config_path: Optional[str] = None) -> Dict[str, Any]:
    """Load configuration from a file or return default empty config.
    
    Args:
        config_path: Path to config file (JSON or YAML). If None, looks for
                     'allocator_config.json' or 'allocator_config.yaml' in:
                     1. Current directory
                     2. Same directory as script
                     3. 'config' subdirectory
    
    Returns:
        Dictionary with configuration options
    
    Raises:
        FileNotFoundError: If config_path is specified but file doesn't exist
        ValueError: If file format is invalid
    """
    if config_path is None:
        # Try to find config file automatically
        possible_paths = [
            Path('allocator_config.json'),
            Path('allocator_config.yaml'),
            Path('allocator_config.yml'),
            Path(__file__).parent / 'allocator_config.json',
            Path(__file__).parent / 'allocator_config.yaml',
            Path(__file__).parent / 'allocator_config.yml',
            Path(__file__).parent / 'config' / 'allocator_config.json',
            Path(__file__).parent / 'config' / 'allocator_config.yaml',
            Path(__file__).parent / 'config' / 'allocator_config.yml',
        ]
        
        for path in possible_paths:
            if path.exists():
                config_path = str(path)
                break
        
        if config_path is None:
            # No config file found, return empty dict (uses defaults)
            return {}
    
    config_file = Path(config_path)
    
    if not config_file.exists():
        raise FileNotFoundError(f"Configuration file not found: {config_path}")
    
    # Determine file type by extension
    suffix = config_file.suffix.lower()
    
    if suffix == '.json':
        with open(config_file, 'r', encoding='utf-8') as f:
            config = json.load(f)
    elif suffix in ['.yaml', '.yml']:
        if not YAML_AVAILABLE:
            raise ImportError(
                "YAML support requires 'pyyaml' package. "
                "Install with: pip install pyyaml"
            )
        with open(config_file, 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
    else:
        raise ValueError(
            f"Unsupported config file format: {suffix}. "
            "Supported formats: .json, .yaml, .yml"
        )
    
    if not isinstance(config, dict):
        raise ValueError(
            f"Configuration file must contain a dictionary/object, "
            f"got {type(config).__name__}"
        )
    
    return config


def merge_config(user_config: Dict[str, Any], default_config: Dict[str, Any]) -> Dict[str, Any]:
    """Merge user config with defaults, with user config taking precedence.
    
    Args:
        user_config: User-provided configuration
        default_config: Default configuration
    
    Returns:
        Merged configuration dictionary
    """
    merged = default_config.copy()
    merged.update(user_config)
    return merged


def validate_config(config: Dict[str, Any]) -> None:
    """Validate configuration values and raise errors for invalid settings.
    
    Args:
        config: Configuration dictionary to validate
    
    Raises:
        ValueError: If any configuration value is invalid
    """
    # Validate boolean options
    bool_options = [
        'maximize_budget_utilization',
        'allow_allocation_without_skills',
        'allow_skill_development',
        'discrete_allocations',
        'budget_flexibility',
        'enable_team_diversity',
        'enable_employee_preferences',
        'enforce_role_allocation',
    ]
    
    for opt in bool_options:
        if opt in config and not isinstance(config[opt], bool):
            raise ValueError(f"{opt} must be a boolean, got {type(config[opt]).__name__}")
    
    # Validate numeric ranges
    if 'max_employee_per_project' in config:
        val = config['max_employee_per_project']
        if not isinstance(val, (int, float)) or val < 0 or val > 1:
            raise ValueError(f"max_employee_per_project must be between 0 and 1, got {val}")
    
    if 'min_team_size' in config:
        val = config['min_team_size']
        if not isinstance(val, int) or val < 0:
            raise ValueError(f"min_team_size must be a non-negative integer, got {val}")
    
    if 'budget_maximization_weight_multiplier' in config:
        val = config['budget_maximization_weight_multiplier']
        if not isinstance(val, (int, float)) or val < 0:
            raise ValueError(f"budget_maximization_weight_multiplier must be >= 0, got {val}")
    
    if 'min_budget_utilization' in config:
        val = config['min_budget_utilization']
        if not isinstance(val, (int, float)) or val < 0 or val > 1:
            raise ValueError(f"min_budget_utilization must be between 0 and 1, got {val}")
    
    if 'no_skills_penalty_multiplier' in config:
        val = config['no_skills_penalty_multiplier']
        if not isinstance(val, (int, float)) or val < 0:
            raise ValueError(f"no_skills_penalty_multiplier must be >= 0, got {val}")
    
    if 'skill_dev_max_fte' in config:
        val = config['skill_dev_max_fte']
        if not isinstance(val, (int, float)) or val < 0 or val > 1:
            raise ValueError(f"skill_dev_max_fte must be between 0 and 1, got {val}")
    
    # Validate dictionaries
    if 'min_role_allocation' in config:
        val = config['min_role_allocation']
        if not isinstance(val, dict):
            raise ValueError(f"min_role_allocation must be a dictionary, got {type(val).__name__}")
        for role, amount in val.items():
            if not isinstance(amount, (int, float)) or amount < 0:
                raise ValueError(f"min_role_allocation[{role}] must be >= 0, got {amount}")
    
    if 'role_allocation_ratios' in config:
        val = config['role_allocation_ratios']
        if not isinstance(val, dict):
            raise ValueError(f"role_allocation_ratios must be a dictionary, got {type(val).__name__}")
        total = sum(val.values())
        if abs(total - 1.0) > 0.01:  # Allow small floating point errors
            raise ValueError(f"role_allocation_ratios must sum to 1.0, got {total}")
    
    if 'allocation_increments' in config:
        val = config['allocation_increments']
        if not isinstance(val, list):
            raise ValueError(f"allocation_increments must be a list, got {type(val).__name__}")
        for inc in val:
            if not isinstance(inc, (int, float)) or inc < 0 or inc > 1:
                raise ValueError(f"allocation_increments values must be between 0 and 1, got {inc}")


def get_config(config_path: Optional[str] = None, validate: bool = True) -> Dict[str, Any]:
    """Load and optionally validate configuration.
    
    Args:
        config_path: Path to config file (optional)
        validate: Whether to validate the configuration
    
    Returns:
        Configuration dictionary
    """
    config = load_config(config_path)
    
    if validate and config:
        validate_config(config)
    
    return config

