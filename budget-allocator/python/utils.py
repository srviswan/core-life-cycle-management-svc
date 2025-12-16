"""utils.py - Helper functions for budget allocator"""
import pandas as pd
import json
import re
from datetime import datetime
from typing import Dict, List, Tuple, Optional
from config import PRIORITY_WEIGHTS


def months_range(start_date: str, end_date: str) -> List[str]:
    """Return list of YYYY-MM strings inclusive between start_date and end_date.
    
    Args:
        start_date: Start date in format YYYY-MM-DD or YYYY-MM
        end_date: End date in format YYYY-MM-DD or YYYY-MM
    
    Returns:
        List of month strings in YYYY-MM format
    """
    # Normalize to YYYY-MM format
    if len(start_date) == 10:  # YYYY-MM-DD
        start_date = start_date[:7]
    if len(end_date) == 10:  # YYYY-MM-DD
        end_date = end_date[:7]
    
    start = pd.to_datetime(start_date + "-01")
    end = pd.to_datetime(end_date + "-01")
    months = pd.date_range(start=start, end=end, freq='MS').strftime('%Y-%m').tolist()
    return months


def parse_available_months(available_months_str: str, all_months: List[str]) -> set:
    """Parse available_months string and return set of available months.
    
    Supports formats:
    - "2025-01,2025-02,2025-03" (comma-separated specific months)
    - "2025-01:2025-06" (range)
    - "2025-01:2025-03,2025-07:2025-12" (multiple ranges)
    - Empty or "all" = all months available
    
    Args:
        available_months_str: String with available months specification
        all_months: List of all months in YYYY-MM format (for validation)
    
    Returns:
        Set of available month strings in YYYY-MM format
    """
    if not available_months_str or pd.isna(available_months_str):
        # Empty or missing = available for all months
        return set(all_months)
    
    available_months_str = str(available_months_str).strip()
    
    if available_months_str.lower() == 'all' or available_months_str == '':
        return set(all_months)
    
    available = set()
    
    # Split by comma to handle multiple ranges/lists
    parts = [p.strip() for p in available_months_str.split(',')]
    
    for part in parts:
        if ':' in part:
            # Range format: "2025-01:2025-06"
            start_str, end_str = part.split(':', 1)
            start_str = start_str.strip()
            end_str = end_str.strip()
            
            # Normalize to YYYY-MM format
            if len(start_str) == 10:
                start_str = start_str[:7]
            if len(end_str) == 10:
                end_str = end_str[:7]
            
            try:
                start = pd.to_datetime(start_str + "-01")
                end = pd.to_datetime(end_str + "-01")
                range_months = pd.date_range(start=start, end=end, freq='MS').strftime('%Y-%m').tolist()
                available.update(range_months)
            except:
                # Invalid range, skip
                continue
        else:
            # Single month: "2025-01"
            month_str = part.strip()
            if len(month_str) == 10:
                month_str = month_str[:7]
            
            # Validate format
            if len(month_str) == 7 and month_str[4] == '-':
                available.add(month_str)
    
    # Filter to only include months that exist in all_months
    return available.intersection(set(all_months))


def _is_regex_pattern(skill: str) -> bool:
    """Check if a skill string is a regex pattern.
    
    Detects regex by checking for common regex characters or 'regex:' prefix.
    """
    skill = skill.strip()
    if skill.startswith('regex:'):
        return True
    # Check for regex special characters
    regex_chars = ['*', '+', '?', '^', '$', '[', ']', '(', ')', '{', '}', '|', '\\']
    return any(char in skill for char in regex_chars)


def _match_skill(skill_pattern: str, resource_skills: str) -> bool:
    """Match a skill pattern (regex or literal) against resource skills.
    
    Args:
        skill_pattern: Skill pattern (may be regex or literal)
        resource_skills: Comma-separated resource skills string
    
    Returns:
        True if pattern matches any skill in resource_skills
    """
    skill_pattern = skill_pattern.strip()
    
    # Remove 'regex:' prefix if present
    if skill_pattern.startswith('regex:'):
        skill_pattern = skill_pattern[6:].strip()
        is_regex = True
    else:
        is_regex = _is_regex_pattern(skill_pattern)
    
    if is_regex:
        try:
            # Compile and match regex pattern
            pattern = re.compile(skill_pattern, re.IGNORECASE)
            # Split resource skills and check each
            resource_skill_list = [s.strip() for s in resource_skills.split(',') if s.strip()]
            return any(pattern.search(skill) for skill in resource_skill_list)
        except re.error:
            # Invalid regex, fall back to literal match
            skill_lower = skill_pattern.lower()
            return skill_lower in resource_skills.lower()
    else:
        # Literal match (case-insensitive substring)
        skill_lower = skill_pattern.lower()
        return skill_lower in resource_skills.lower()


def _parse_simple_skill_string(skill_str: str) -> Dict[str, List[str]]:
    """Parse a simple skill string with AND/OR operators.
    
    Supports formats:
    - "python,java" (comma = AND, all required)
    - "python|java" (pipe = OR, at least one required)
    - "python,java|sql" (mixed: (python AND java) OR sql)
    - "python&java|sql" (mixed: (python AND java) OR sql)
    - "regex:python.*" (regex pattern)
    - "python*" (regex wildcard)
    
    Note: For complex nested AND/OR, we simplify to OR (at least one skill matches).
    For true AND logic, use comma-separated without |.
    
    Returns dict with 'and' and 'or' lists.
    """
    if not skill_str or not skill_str.strip():
        return {'and': [], 'or': []}
    
    skill_str = skill_str.strip()
    
    # If contains |, split by | first (OR has lower precedence)
    if '|' in skill_str:
        or_parts = [part.strip() for part in skill_str.split('|') if part.strip()]
        
        and_skills = []
        or_skills = []
        
        for part in or_parts:
            # Check if this part contains & or , (AND operators)
            if '&' in part:
                # Split by & to get AND skills
                and_skills_in_part = [s.strip() for s in part.split('&') if s.strip()]
                # When in OR context, treat AND group as requiring all skills
                # But for simplicity, we'll add all to OR (any one matches)
                or_skills.extend(and_skills_in_part)
            elif ',' in part:
                # Split by comma to get AND skills
                and_skills_in_part = [s.strip() for s in part.split(',') if s.strip()]
                # When in OR context, treat AND group as requiring all skills
                # But for simplicity, we'll add all to OR (any one matches)
                or_skills.extend(and_skills_in_part)
            else:
                # Single skill (OR)
                or_skills.append(part)
        
        return {
            'and': and_skills,
            'or': or_skills if or_skills else []
        }
    else:
        # No |, so check for & or , (AND operators)
        if '&' in skill_str:
            # Split by & to get AND skills
            and_skills = [s.strip() for s in skill_str.split('&') if s.strip()]
            return {'and': and_skills, 'or': []}
        elif ',' in skill_str:
            # Split by comma to get AND skills
            and_skills = [s.strip() for s in skill_str.split(',') if s.strip()]
            return {'and': and_skills, 'or': []}
        else:
            # Single skill - treat as AND (required)
            return {'and': [skill_str.strip()], 'or': []}


def parse_required_skills(field) -> Dict[str, List[str]]:
    """Parse required skills from project data with simplified AND/OR and regex support.
    
    Supports two formats:
    1. **Simple string format** (recommended):
       - "python,java" (comma = AND, all required)
       - "python|java" (pipe = OR, at least one)
       - "python&java|sql" (mixed: (python AND java) OR sql)
       - "regex:python.*" or "python*" (regex patterns)
    
    2. **JSON format** (legacy, still supported):
       - 'technical', 'functional', 'mandatory' (comma-separated or list)
       - 'technical_and', 'functional_and', 'mandatory_and' (ALL required)
       - 'technical_or', 'functional_or', 'mandatory_or' (AT LEAST ONE required)
    
    Args:
        field: String (simple format or JSON) or dict
    
    Returns:
        Dictionary with skill lists including AND/OR operators
    """
    if not field or pd.isna(field):
        return {
            'technical': [], 'functional': [], 'mandatory': [],
            'technical_and': [], 'technical_or': [],
            'functional_and': [], 'functional_or': [],
            'mandatory_and': [], 'mandatory_or': []
        }
    
    # Try JSON format first (if it looks like JSON)
    if isinstance(field, str):
        field_stripped = field.strip()
        if field_stripped.startswith('{') and field_stripped.endswith('}'):
            try:
                val = json.loads(field)
                # Process JSON format
                result = {
                    'technical': val.get('technical', []),
                    'functional': val.get('functional', []),
                    'mandatory': val.get('mandatory', []),
                    'technical_and': val.get('technical_and', []),
                    'technical_or': val.get('technical_or', []),
                    'functional_and': val.get('functional_and', []),
                    'functional_or': val.get('functional_or', []),
                    'mandatory_and': val.get('mandatory_and', []),
                    'mandatory_or': val.get('mandatory_or', [])
                }
                
                # Convert string lists to actual lists
                for key in ['technical', 'functional', 'mandatory']:
                    if isinstance(result[key], str):
                        parsed = _parse_simple_skill_string(result[key])
                        result[f'{key}_and'].extend(parsed['and'])
                        result[f'{key}_or'].extend(parsed['or'] if isinstance(parsed['or'], list) else [parsed['or']])
                        result[key] = []
                
                # Legacy: merge 'technical', 'functional', 'mandatory' into _and
                if result['technical']:
                    if isinstance(result['technical'], list):
                        result['technical_and'].extend(result['technical'])
                    else:
                        parsed = _parse_simple_skill_string(str(result['technical']))
                        result['technical_and'].extend(parsed['and'])
                        result['technical_or'].extend(parsed['or'] if isinstance(parsed['or'], list) else [parsed['or']])
                    result['technical'] = []
                
                if result['functional']:
                    if isinstance(result['functional'], list):
                        result['functional_and'].extend(result['functional'])
                    else:
                        parsed = _parse_simple_skill_string(str(result['functional']))
                        result['functional_and'].extend(parsed['and'])
                        result['functional_or'].extend(parsed['or'] if isinstance(parsed['or'], list) else [parsed['or']])
                    result['functional'] = []
                
                if result['mandatory']:
                    if isinstance(result['mandatory'], list):
                        result['mandatory_and'].extend(result['mandatory'])
                    else:
                        parsed = _parse_simple_skill_string(str(result['mandatory']))
                        result['mandatory_and'].extend(parsed['and'])
                        result['mandatory_or'].extend(parsed['or'] if isinstance(parsed['or'], list) else [parsed['or']])
                    result['mandatory'] = []
                
                return result
            except Exception:
                pass  # Not valid JSON, try simple format
        
        # Simple string format - treat as technical skills
        parsed = _parse_simple_skill_string(field)
        return {
            'technical': [],
            'functional': [],
            'mandatory': [],
            'technical_and': parsed['and'],
            'technical_or': [item if isinstance(item, str) else item[0] for item in parsed['or']] if parsed['or'] else [],
            'functional_and': [],
            'functional_or': [],
            'mandatory_and': [],
            'mandatory_or': []
        }
    elif isinstance(field, dict):
        # Process dict format (same as JSON)
        val = field
        result = {
            'technical': val.get('technical', []),
            'functional': val.get('functional', []),
            'mandatory': val.get('mandatory', []),
            'technical_and': val.get('technical_and', []),
            'technical_or': val.get('technical_or', []),
            'functional_and': val.get('functional_and', []),
            'functional_or': val.get('functional_or', []),
            'mandatory_and': val.get('mandatory_and', []),
            'mandatory_or': val.get('mandatory_or', [])
        }
        
        # Convert string fields to parsed format
        for key in ['technical', 'functional', 'mandatory']:
            if isinstance(result[key], str):
                parsed = _parse_simple_skill_string(result[key])
                result[f'{key}_and'].extend(parsed['and'])
                result[f'{key}_or'].extend(parsed['or'] if isinstance(parsed['or'], list) else [parsed['or']])
                result[key] = []
        
        # Legacy support
        if result['technical']:
            result['technical_and'].extend(result['technical'] if isinstance(result['technical'], list) else [result['technical']])
            result['technical'] = []
        if result['functional']:
            result['functional_and'].extend(result['functional'] if isinstance(result['functional'], list) else [result['functional']])
            result['functional'] = []
        if result['mandatory']:
            result['mandatory_and'].extend(result['mandatory'] if isinstance(result['mandatory'], list) else [result['mandatory']])
            result['mandatory'] = []
        
        return result
    else:
        return {
            'technical': [], 'functional': [], 'mandatory': [],
            'technical_and': [], 'technical_or': [],
            'functional_and': [], 'functional_or': [],
            'mandatory_and': [], 'mandatory_or': []
        }


def normalize_driver(driver: str) -> float:
    """Normalize driver to 0-1 scale.
    
    Common drivers: Regulatory, Strategic, Product, Operational, etc.
    Higher priority drivers get higher scores.
    """
    if pd.isna(driver) or not driver:
        return 0.5  # Default medium
    
    driver_lower = str(driver).lower().strip()
    
    # Priority mapping (can be customized)
    driver_map = {
        'regulatory': 1.0,
        'strategic': 0.9,
        'compliance': 0.95,
        'product': 0.7,
        'operational': 0.5,
        'maintenance': 0.3,
        'research': 0.4,
    }
    
    # Check for exact match or contains
    for key, value in driver_map.items():
        if key in driver_lower:
            return value
    
    # Default: map to numeric if possible
    try:
        num_val = float(driver)
        return min(max(num_val / 10.0, 0.0), 1.0)  # Assume 0-10 scale
    except:
        return 0.5  # Default medium


def normalize_impact(impact: str) -> float:
    """Normalize impact to 0-1 scale.
    
    High=1.0, Medium=0.5, Low=0.0
    """
    if pd.isna(impact) or not impact:
        return 0.5  # Default medium
    
    impact_lower = str(impact).lower().strip()
    
    impact_map = {
        'high': 1.0,
        'medium': 0.5,
        'low': 0.0,
        'critical': 1.0,
        'important': 0.8,
        'minor': 0.2,
    }
    
    for key, value in impact_map.items():
        if key in impact_lower:
            return value
    
    # Try numeric
    try:
        num_val = float(impact)
        return min(max(num_val / 10.0, 0.0), 1.0)
    except:
        return 0.5


def normalize_rank(rank) -> float:
    """Normalize rank to 0-1 scale.
    
    Higher rank number = higher priority = higher score
    Assumes rank 1 is highest priority.
    """
    if pd.isna(rank):
        return 0.5  # Default medium
    
    try:
        rank_num = float(rank)
        if rank_num <= 0:
            return 0.0
        # Invert: rank 1 = 1.0, rank 10 = 0.1 (assuming max rank 10)
        # Formula: 1.0 - (rank - 1) / max_rank
        max_rank = 20  # Assume max rank of 20
        normalized = 1.0 - (rank_num - 1) / max_rank
        return max(0.0, min(1.0, normalized))
    except:
        return 0.5


def calculate_project_priority(project_row: pd.Series, weights: Optional[Dict[str, float]] = None) -> float:
    """Calculate project priority using weighted sum.
    
    Args:
        project_row: Project data row with 'driver', 'impact', 'rank' fields
        weights: Optional weights dict, defaults to PRIORITY_WEIGHTS from config
    
    Returns:
        Priority score (0.0 to 1.0, higher = higher priority)
    """
    if weights is None:
        weights = PRIORITY_WEIGHTS
    
    driver = project_row.get('driver', '')
    impact = project_row.get('impact', 'Medium')
    rank = project_row.get('rank', None)
    
    driver_score = normalize_driver(driver)
    impact_score = normalize_impact(impact)
    rank_score = normalize_rank(rank)
    
    priority = (
        weights['driver'] * driver_score +
        weights['impact'] * impact_score +
        weights['rank'] * rank_score
    )
    
    return priority


def check_mandatory_skills(resource_row: pd.Series, project_skills: Dict[str, List[str]]) -> Tuple[bool, Dict]:
    """Check if resource meets skill requirements with AND/OR logic and regex support.
    
    HARD CONSTRAINTS (must be met to allocate):
    - 'mandatory_and': ALL mandatory skills required (AND) - supports regex
    - 'mandatory_or': AT LEAST ONE mandatory skill required (OR) - supports regex
    
    SOFT CONSTRAINTS (preferences, affect score but don't block allocation):
    - 'technical_and': ALL technical skills preferred (AND) - supports regex
    - 'technical_or': AT LEAST ONE technical skill preferred (OR) - supports regex
    - 'functional_and': ALL functional skills preferred (AND) - supports regex
    - 'functional_or': AT LEAST ONE functional skill preferred (OR) - supports regex
    
    Args:
        resource_row: Resource data row with 'technical_skills', 'functional_skills'
        project_skills: Project skills dict
    
    Returns:
        Tuple of (can_allocate: bool, match_info: dict)
        match_info contains: matched_skills, missing_skills, match_details
    """
    resource_tech = str(resource_row.get('technical_skills', '') or '')
    resource_func = str(resource_row.get('functional_skills', '') or '')
    resource_all_skills = resource_tech + ',' + resource_func
    
    # HARD CONSTRAINTS: Only mandatory skills block allocation
    mandatory_and = project_skills.get('mandatory_and', [])
    mandatory_or = project_skills.get('mandatory_or', [])
    
    # Check mandatory AND (all must be present) - HARD CONSTRAINT
    matched_mandatory_and = []
    missing_mandatory_and = []
    
    for skill in mandatory_and:
        if _match_skill(str(skill), resource_all_skills):
            matched_mandatory_and.append(skill)
        else:
            missing_mandatory_and.append(skill)
    
    # Check mandatory OR (at least one must be present) - HARD CONSTRAINT
    mandatory_or_met = False
    if not mandatory_or:
        mandatory_or_met = True  # No OR requirement = satisfied
    else:
        for skill in mandatory_or:
            if _match_skill(str(skill), resource_all_skills):
                mandatory_or_met = True
                break
    
    # SOFT CONSTRAINTS: Technical and functional skills (for scoring, not blocking)
    technical_and = project_skills.get('technical_and', [])
    technical_or = project_skills.get('technical_or', [])
    functional_and = project_skills.get('functional_and', [])
    functional_or = project_skills.get('functional_or', [])
    
    # Check technical AND (for scoring)
    matched_technical_and = []
    missing_technical_and = []
    for skill in technical_and:
        if _match_skill(str(skill), resource_tech):
            matched_technical_and.append(skill)
        else:
            missing_technical_and.append(skill)
    
    # Check technical OR (for scoring)
    technical_or_met = False
    if not technical_or:
        technical_or_met = True  # No OR requirement = satisfied
    else:
        for skill in technical_or:
            if _match_skill(str(skill), resource_tech):
                technical_or_met = True
                break
    
    # Check functional AND (for scoring)
    matched_functional_and = []
    missing_functional_and = []
    for skill in functional_and:
        if _match_skill(str(skill), resource_func):
            matched_functional_and.append(skill)
        else:
            missing_functional_and.append(skill)
    
    # Check functional OR (for scoring)
    functional_or_met = False
    if not functional_or:
        functional_or_met = True  # No OR requirement = satisfied
    else:
        for skill in functional_or:
            if _match_skill(str(skill), resource_func):
                functional_or_met = True
                break
    
    # Can allocate if: all MANDATORY AND requirements met AND all MANDATORY OR requirements met
    # Technical and functional skills are SOFT constraints (don't block allocation)
    can_allocate = (len(missing_mandatory_and) == 0) and mandatory_or_met
    
    # Collect all matched/missing for reporting
    all_matched = matched_mandatory_and + matched_technical_and + matched_functional_and
    all_missing = missing_mandatory_and + missing_technical_and + missing_functional_and
    
    match_details_parts = []
    if mandatory_and:
        match_details_parts.append(f"Mandatory AND: {len(matched_mandatory_and)}/{len(mandatory_and)}")
    if mandatory_or:
        match_details_parts.append(f"Mandatory OR: {'met' if mandatory_or_met else 'not met'}")
    if technical_and:
        match_details_parts.append(f"Technical AND: {len(matched_technical_and)}/{len(technical_and)}")
    if technical_or:
        match_details_parts.append(f"Technical OR: {'met' if technical_or_met else 'not met'}")
    if functional_and:
        match_details_parts.append(f"Functional AND: {len(matched_functional_and)}/{len(functional_and)}")
    if functional_or:
        match_details_parts.append(f"Functional OR: {'met' if functional_or_met else 'not met'}")
    
    match_details = ', '.join(match_details_parts) if match_details_parts else 'No skill requirements'
    
    return can_allocate, {
        'matched_skills': all_matched,
        'missing_skills': all_missing,
        'mandatory_and_matched': matched_mandatory_and,
        'mandatory_and_missing': missing_mandatory_and,
        'technical_and_matched': matched_technical_and,
        'technical_and_missing': missing_technical_and,
        'functional_and_matched': matched_functional_and,
        'functional_and_missing': missing_functional_and,
        'mandatory_or_met': mandatory_or_met,
        'technical_or_met': technical_or_met,
        'functional_or_met': functional_or_met,
        'match_details': match_details
    }


def calculate_skill_match_score(resource_row: pd.Series, project_skills: Dict[str, List[str]]) -> Dict[str, float]:
    """Calculate skill match score for technical and functional skills with AND/OR and regex support.
    
    Args:
        resource_row: Resource data row
        project_skills: Project skills dict with AND/OR operators and regex support
    
    Returns:
        Dict with 'technical_score', 'functional_score', 'overall_score' (0.0 to 1.0)
    """
    resource_tech = str(resource_row.get('technical_skills', '') or '')
    resource_func = str(resource_row.get('functional_skills', '') or '')
    
    # Get AND requirements (all must match) - supports regex
    req_tech_and = [str(s).strip() for s in project_skills.get('technical_and', [])]
    req_func_and = [str(s).strip() for s in project_skills.get('functional_and', [])]
    
    # Get OR requirements (at least one must match) - supports regex
    req_tech_or = [str(s).strip() for s in project_skills.get('technical_or', [])]
    req_func_or = [str(s).strip() for s in project_skills.get('functional_or', [])]
    
    # Calculate technical match
    tech_and_matches = 0
    if req_tech_and:
        for skill in req_tech_and:
            if _match_skill(skill, resource_tech):
                tech_and_matches += 1
        tech_and_score = tech_and_matches / len(req_tech_and) if req_tech_and else 1.0
    else:
        tech_and_score = 1.0  # No AND requirements = perfect
    
    tech_or_matches = 0
    if req_tech_or:
        for skill in req_tech_or:
            if _match_skill(skill, resource_tech):
                tech_or_matches += 1
        tech_or_score = 1.0 if tech_or_matches > 0 else 0.0  # At least one = 1.0, none = 0.0
    else:
        tech_or_score = 1.0  # No OR requirements = perfect
    
    # Combined technical score (both AND and OR must be satisfied)
    tech_score = min(tech_and_score, tech_or_score)
    
    # Calculate functional match
    func_and_matches = 0
    if req_func_and:
        for skill in req_func_and:
            if _match_skill(skill, resource_func):
                func_and_matches += 1
        func_and_score = func_and_matches / len(req_func_and) if req_func_and else 1.0
    else:
        func_and_score = 1.0  # No AND requirements = perfect
    
    func_or_matches = 0
    if req_func_or:
        for skill in req_func_or:
            if _match_skill(skill, resource_func):
                func_or_matches += 1
        func_or_score = 1.0 if func_or_matches > 0 else 0.0  # At least one = 1.0, none = 0.0
    else:
        func_or_score = 1.0  # No OR requirements = perfect
    
    # Combined functional score
    func_score = min(func_and_score, func_or_score)
    
    # Overall score (weighted average: technical 60%, functional 40%)
    overall_score = 0.6 * tech_score + 0.4 * func_score
    
    return {
        'technical_score': tech_score,
        'functional_score': func_score,
        'overall_score': overall_score,
        'technical_and_matches': tech_and_matches,
        'technical_or_matches': tech_or_matches,
        'functional_and_matches': func_and_matches,
        'functional_or_matches': func_or_matches
    }


def calculate_effort_alignment(allocated_cost: float, resource_monthly_cost: float, 
                              effort_estimate: Optional[float], project_months: int) -> float:
    """Calculate how well allocation aligns with effort estimate.
    
    Args:
        allocated_cost: Cost allocated for this resource-project-month
        resource_monthly_cost: Monthly cost of resource
        effort_estimate: Project effort estimate in man-months (optional)
        project_months: Number of months in project
    
    Returns:
        Alignment score (0.0 to 1.0, higher = better alignment)
    """
    if not effort_estimate or effort_estimate <= 0:
        return 0.5  # Neutral if no estimate
    
    # Calculate allocated man-months
    allocated_man_months = (allocated_cost / resource_monthly_cost) if resource_monthly_cost > 0 else 0.0
    
    # Target man-months per month
    target_per_month = effort_estimate / project_months if project_months > 0 else 0.0
    
    if target_per_month == 0:
        return 0.5
    
    # Calculate alignment (how close allocated is to target)
    ratio = allocated_man_months / target_per_month if target_per_month > 0 else 0.0
    
    # Score: 1.0 if ratio = 1.0, decreases as ratio deviates
    # Use exponential decay: score = exp(-abs(ratio - 1.0))
    import math
    alignment = math.exp(-abs(ratio - 1.0))
    
    return alignment


def generate_allocation_explanation(resource_id: str, resource_name: str, project_id: int, 
                                   project_name: str, allocated_cost: float, priority_score: float,
                                   skill_scores: Dict[str, float], effort_alignment: float,
                                   is_efficiency_project: bool, project_rank: Optional[int] = None) -> str:
    """Generate human-readable explanation for an allocation.
    
    Args:
        resource_id: Resource BRID
        resource_name: Resource name
        project_id: Project ID
        project_name: Project name
        allocated_cost: Cost allocated
        priority_score: Project priority score
        skill_scores: Skill match scores dict
        effort_alignment: Effort alignment score
        is_efficiency_project: Whether project has no budget
        project_rank: Optional project rank
    
    Returns:
        Human-readable explanation string
    """
    parts = []
    
    # Priority information
    if project_rank:
        parts.append(f"Project ranked #{project_rank}")
    if priority_score >= 0.8:
        parts.append("high priority project")
    elif priority_score >= 0.5:
        parts.append("medium priority project")
    else:
        parts.append("lower priority project")
    parts.append(f"(priority score: {priority_score:.2f})")
    
    # Skill match
    overall_skill = skill_scores.get('overall_score', 0.0)
    if overall_skill >= 0.9:
        parts.append("perfect skill match")
    elif overall_skill >= 0.7:
        parts.append("good skill match")
    elif overall_skill >= 0.5:
        parts.append("moderate skill match")
    else:
        parts.append("basic skill match")
    parts.append(f"(skill score: {overall_skill:.0%})")
    
    # Mandatory skills
    if skill_scores.get('mandatory_met', True):
        parts.append("all mandatory skills met")
    
    # Effort alignment
    if effort_alignment >= 0.8:
        parts.append("effort estimate well-aligned")
    elif effort_alignment >= 0.5:
        parts.append("effort estimate reasonably aligned")
    
    # Efficiency project
    if is_efficiency_project:
        parts.append("efficiency allocation (no budget project, using unallocated resource)")
    
    # Waterfall
    if priority_score >= 0.7:
        parts.append("waterfall allocation - higher priority than alternatives")
    
    explanation = ", ".join(parts)
    return explanation.capitalize() + "."
