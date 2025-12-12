"""utils.py - Helper functions for budget allocator"""
import pandas as pd
import json
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


def parse_required_skills(field) -> Dict[str, List[str]]:
    """Parse required skills from project data.
    
    Args:
        field: JSON string or dict with 'technical', 'functional', 'mandatory' keys
    
    Returns:
        Dictionary with 'technical', 'functional', 'mandatory' skill lists
    """
    if not field or pd.isna(field):
        return {'technical': [], 'functional': [], 'mandatory': []}
    
    if isinstance(field, str):
        try:
            val = json.loads(field)
            return {
                'technical': val.get('technical', []),
                'functional': val.get('functional', []),
                'mandatory': val.get('mandatory', [])
            }
        except Exception:
            # Fallback: assume comma-separated technical skills
            return {
                'technical': [s.strip() for s in field.split(',') if s.strip()],
                'functional': [],
                'mandatory': []
            }
    
    if isinstance(field, dict):
        return {
            'technical': field.get('technical', []),
            'functional': field.get('functional', []),
            'mandatory': field.get('mandatory', [])
        }
    
    return {'technical': [], 'functional': [], 'mandatory': []}


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
    """Check if resource has ALL mandatory skills (hard constraint).
    
    Args:
        resource_row: Resource data row with 'technical_skills', 'functional_skills'
        project_skills: Project skills dict with 'mandatory', 'technical', 'functional' keys
    
    Returns:
        Tuple of (can_allocate: bool, match_info: dict)
        match_info contains: matched_skills, missing_skills, match_details
    """
    mandatory_skills = project_skills.get('mandatory', [])
    
    if not mandatory_skills:
        # No mandatory skills = can allocate
        return True, {
            'matched_skills': [],
            'missing_skills': [],
            'match_details': 'No mandatory skills required'
        }
    
    resource_tech = str(resource_row.get('technical_skills', '') or '').lower()
    resource_func = str(resource_row.get('functional_skills', '') or '').lower()
    resource_all_skills = resource_tech + ',' + resource_func
    
    matched = []
    missing = []
    
    for skill in mandatory_skills:
        skill_lower = str(skill).lower().strip()
        # Check if skill appears in resource skills (case-insensitive)
        if skill_lower in resource_all_skills:
            matched.append(skill)
        else:
            missing.append(skill)
    
    can_allocate = len(missing) == 0
    
    return can_allocate, {
        'matched_skills': matched,
        'missing_skills': missing,
        'match_details': f"Matched {len(matched)}/{len(mandatory_skills)} mandatory skills"
    }


def calculate_skill_match_score(resource_row: pd.Series, project_skills: Dict[str, List[str]]) -> Dict[str, float]:
    """Calculate skill match score for technical and functional skills.
    
    Args:
        resource_row: Resource data row
        project_skills: Project skills dict
    
    Returns:
        Dict with 'technical_score', 'functional_score', 'overall_score' (0.0 to 1.0)
    """
    resource_tech = str(resource_row.get('technical_skills', '') or '').lower()
    resource_func = str(resource_row.get('functional_skills', '') or '').lower()
    
    req_tech = [s.lower().strip() for s in project_skills.get('technical', [])]
    req_func = [s.lower().strip() for s in project_skills.get('functional', [])]
    
    # Calculate technical match
    tech_matches = 0
    if req_tech:
        for skill in req_tech:
            if skill in resource_tech:
                tech_matches += 1
        tech_score = tech_matches / len(req_tech) if req_tech else 0.0
    else:
        tech_score = 1.0  # No requirements = perfect match
    
    # Calculate functional match
    func_matches = 0
    if req_func:
        for skill in req_func:
            if skill in resource_func:
                func_matches += 1
        func_score = func_matches / len(req_func) if req_func else 0.0
    else:
        func_score = 1.0  # No requirements = perfect match
    
    # Overall score (weighted average: technical 60%, functional 40%)
    overall_score = 0.6 * tech_score + 0.4 * func_score
    
    return {
        'technical_score': tech_score,
        'functional_score': func_score,
        'overall_score': overall_score,
        'technical_matches': tech_matches,
        'functional_matches': func_matches
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
