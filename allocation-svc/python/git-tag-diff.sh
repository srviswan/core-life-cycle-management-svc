#!/usr/bin/env bash
set -euo pipefail

# ─────────────────────────────────────────────────────────────────────────────
# git-tag-diff.sh — Compare two Git tags and surface key areas to review
# ─────────────────────────────────────────────────────────────────────────────
# Usage:
#   ./git-tag-diff.sh <old_tag> <new_tag> [--repo /path/to/repo] [--output report.txt]
#
# Examples:
#   ./git-tag-diff.sh v1.2.0 v1.3.0
#   ./git-tag-diff.sh v1.2.0 v1.3.0 --repo /opt/myproject --output diff-report.txt
# ─────────────────────────────────────────────────────────────────────────────

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

REPO_PATH="."
OUTPUT_FILE=""

usage() {
    echo "Usage: $0 <old_tag> <new_tag> [--repo /path/to/repo] [--output report.txt]"
    echo ""
    echo "Options:"
    echo "  --repo     Path to the git repository (default: current directory)"
    echo "  --output   Write plain-text report to file"
    exit 1
}

if [[ $# -lt 2 ]]; then
    usage
fi

OLD_TAG="$1"
NEW_TAG="$2"
shift 2

while [[ $# -gt 0 ]]; do
    case "$1" in
        --repo)   REPO_PATH="$2"; shift 2 ;;
        --output) OUTPUT_FILE="$2"; shift 2 ;;
        *)        echo "Unknown option: $1"; usage ;;
    esac
done

if [[ ! -d "$REPO_PATH/.git" ]]; then
    echo "Error: '$REPO_PATH' is not a git repository."
    exit 1
fi

cd "$REPO_PATH"

validate_ref() {
    if ! git rev-parse "$1" >/dev/null 2>&1; then
        echo "Error: Tag or ref '$1' does not exist."
        exit 1
    fi
}

validate_ref "$OLD_TAG"
validate_ref "$NEW_TAG"

REPORT=""
append() { REPORT+="$1"$'\n'; }

section() {
    append ""
    append "$(printf '=%.0s' {1..80})"
    append "  $1"
    append "$(printf '=%.0s' {1..80})"
    append ""
}

subsection() {
    append "  --- $1 ---"
    append ""
}

# ─────────────────────────────────────────────────────────────────────────────
# Header
# ─────────────────────────────────────────────────────────────────────────────
section "GIT TAG COMPARISON REPORT"
append "  Old Tag : $OLD_TAG  ($(git rev-parse --short "$OLD_TAG"))"
append "  New Tag : $NEW_TAG  ($(git rev-parse --short "$NEW_TAG"))"
append "  Generated : $(date '+%Y-%m-%d %H:%M:%S')"
append "  Repository: $(basename "$(git rev-parse --show-toplevel)")"

OLD_DATE=$(git log -1 --format='%ai' "$OLD_TAG" 2>/dev/null || echo "N/A")
NEW_DATE=$(git log -1 --format='%ai' "$NEW_TAG" 2>/dev/null || echo "N/A")
append ""
append "  Old Tag Date: $OLD_DATE"
append "  New Tag Date: $NEW_DATE"

# ─────────────────────────────────────────────────────────────────────────────
# 1. Commit Summary
# ─────────────────────────────────────────────────────────────────────────────
section "1. COMMIT SUMMARY"

COMMIT_COUNT=$(git rev-list --count "$OLD_TAG".."$NEW_TAG")
AUTHOR_COUNT=$(git log "$OLD_TAG".."$NEW_TAG" --format='%aN' | sort -u | wc -l | tr -d ' ')

append "  Total commits : $COMMIT_COUNT"
append "  Unique authors: $AUTHOR_COUNT"
append ""

subsection "Commits by Author"
git log "$OLD_TAG".."$NEW_TAG" --format='%aN' | sort | uniq -c | sort -rn | while read -r line; do
    append "    $line"
done

append ""
subsection "Commit Timeline (first and last)"
FIRST_COMMIT=$(git log "$OLD_TAG".."$NEW_TAG" --format='%h %ai %s' --reverse | head -1)
LAST_COMMIT=$(git log "$OLD_TAG".."$NEW_TAG" --format='%h %ai %s' | head -1)
append "    First: $FIRST_COMMIT"
append "    Last : $LAST_COMMIT"

# ─────────────────────────────────────────────────────────────────────────────
# 2. High-Level Diff Statistics
# ─────────────────────────────────────────────────────────────────────────────
section "2. DIFF STATISTICS"

DIFFSTAT=$(git diff --stat "$OLD_TAG".."$NEW_TAG" | tail -1)
append "  $DIFFSTAT"
append ""

ADDED_FILES=$(git diff --diff-filter=A --name-only "$OLD_TAG".."$NEW_TAG" | wc -l | tr -d ' ')
MODIFIED_FILES=$(git diff --diff-filter=M --name-only "$OLD_TAG".."$NEW_TAG" | wc -l | tr -d ' ')
DELETED_FILES=$(git diff --diff-filter=D --name-only "$OLD_TAG".."$NEW_TAG" | wc -l | tr -d ' ')
RENAMED_FILES=$(git diff --diff-filter=R --name-only "$OLD_TAG".."$NEW_TAG" | wc -l | tr -d ' ')

append "  Added   : $ADDED_FILES files"
append "  Modified: $MODIFIED_FILES files"
append "  Deleted : $DELETED_FILES files"
append "  Renamed : $RENAMED_FILES files"

# ─────────────────────────────────────────────────────────────────────────────
# 3. Changes by Directory (Top-Level Hotspots)
# ─────────────────────────────────────────────────────────────────────────────
section "3. CHANGES BY DIRECTORY (hotspots)"

git diff --name-only "$OLD_TAG".."$NEW_TAG" | awk -F'/' '{
    if (NF > 1) dir=$1"/"$2; else dir=$1
    count[dir]++
}
END {
    for (d in count) printf "    %5d  %s\n", count[d], d
}' | sort -rn | head -20 | while read -r line; do
    append "$line"
done

# ─────────────────────────────────────────────────────────────────────────────
# 4. Changes by File Type
# ─────────────────────────────────────────────────────────────────────────────
section "4. CHANGES BY FILE TYPE"

git diff --name-only "$OLD_TAG".."$NEW_TAG" | awk -F. '{
    if (NF > 1) ext="."$NF; else ext="(no ext)"
    count[ext]++
}
END {
    for (e in count) printf "    %5d  %s\n", count[e], e
}' | sort -rn | while read -r line; do
    append "$line"
done

# ─────────────────────────────────────────────────────────────────────────────
# 5. Most Changed Files (highest churn — key review targets)
# ─────────────────────────────────────────────────────────────────────────────
section "5. MOST CHANGED FILES (top 20 by lines changed)"
append "  These files have the highest churn and should be reviewed carefully."
append ""

git diff --numstat "$OLD_TAG".."$NEW_TAG" | awk '{
    added=$1; removed=$2; file=$3
    if (added == "-") next  # skip binary
    total = added + removed
    printf "    %6d (+%-5d -%−5d)  %s\n", total, added, removed, file
}' | sort -rn | head -20 | while read -r line; do
    append "$line"
done

# ─────────────────────────────────────────────────────────────────────────────
# 6. New Files (potential new features / components)
# ─────────────────────────────────────────────────────────────────────────────
section "6. NEW FILES"

NEW_FILES=$(git diff --diff-filter=A --name-only "$OLD_TAG".."$NEW_TAG")
if [[ -z "$NEW_FILES" ]]; then
    append "  (none)"
else
    echo "$NEW_FILES" | while read -r f; do
        append "    + $f"
    done
fi

# ─────────────────────────────────────────────────────────────────────────────
# 7. Deleted Files (potential breaking changes)
# ─────────────────────────────────────────────────────────────────────────────
section "7. DELETED FILES"

DEL_FILES=$(git diff --diff-filter=D --name-only "$OLD_TAG".."$NEW_TAG")
if [[ -z "$DEL_FILES" ]]; then
    append "  (none)"
else
    append "  *** REVIEW: Deletions may cause breaking changes ***"
    append ""
    echo "$DEL_FILES" | while read -r f; do
        append "    - $f"
    done
fi

# ─────────────────────────────────────────────────────────────────────────────
# 8. Renamed / Moved Files
# ─────────────────────────────────────────────────────────────────────────────
section "8. RENAMED / MOVED FILES"

REN_FILES=$(git diff --diff-filter=R --name-status "$OLD_TAG".."$NEW_TAG")
if [[ -z "$REN_FILES" ]]; then
    append "  (none)"
else
    echo "$REN_FILES" | while read -r status old new; do
        append "    $old  →  $new"
    done
fi

# ─────────────────────────────────────────────────────────────────────────────
# 9. Key Patterns to Watch (automated red-flag detection)
# ─────────────────────────────────────────────────────────────────────────────
section "9. KEY AREAS TO REVIEW (automated red-flag scan)"

append "  Scanning diff for patterns that commonly need careful review..."
append ""

flag_count=0

check_pattern() {
    local label="$1"
    local pattern="$2"
    local matches
    matches=$(git diff "$OLD_TAG".."$NEW_TAG" | grep -cE "$pattern" 2>/dev/null || true)
    if [[ "$matches" -gt 0 ]]; then
        append "  [!] $label — $matches occurrence(s)"
        flag_count=$((flag_count + 1))
    fi
}

# Database / Schema changes
check_pattern "SQL Schema Changes (ALTER TABLE, CREATE TABLE, DROP)" \
    "^\+.*(ALTER TABLE|CREATE TABLE|DROP TABLE|ADD COLUMN|DROP COLUMN|CREATE INDEX|DROP INDEX)"

check_pattern "Migration Files Changed" \
    "^\+.*migration"

# Configuration changes
check_pattern "Config / Environment Changes (.env, .config, .yml, .json config)" \
    "^\+.*(config|\.env|connection.?string|app.?settings)"

CONFIG_FILES=$(git diff --name-only "$OLD_TAG".."$NEW_TAG" | grep -iE '(\.config|\.yml|\.yaml|\.json|\.env|\.properties|appsettings|web\.config)' || true)
if [[ -n "$CONFIG_FILES" ]]; then
    append "  [!] Configuration files modified:"
    echo "$CONFIG_FILES" | while read -r f; do
        append "      - $f"
    done
    flag_count=$((flag_count + 1))
fi

# Security-sensitive patterns
check_pattern "Security-Sensitive (password, secret, token, key, credential)" \
    "^\+.*(password|secret|token|api.?key|credential|private.?key)"

# Dependency changes
DEP_FILES=$(git diff --name-only "$OLD_TAG".."$NEW_TAG" | grep -iE '(package\.json|requirements\.txt|pom\.xml|\.csproj|packages\.config|Gemfile|Cargo\.toml|go\.mod|\.sln)' || true)
if [[ -n "$DEP_FILES" ]]; then
    append "  [!] Dependency files changed (review for version bumps / new deps):"
    echo "$DEP_FILES" | while read -r f; do
        append "      - $f"
    done
    flag_count=$((flag_count + 1))
fi

# Stored procedures / SQL logic
check_pattern "Stored Procedure Changes (CREATE/ALTER PROC)" \
    "^\+.*(CREATE PROC|ALTER PROC|CREATE FUNCTION|ALTER FUNCTION|CREATE TRIGGER)"

# Error handling changes
check_pattern "Exception / Error Handling Changes" \
    "^\+.*(try|catch|throw|raise|except|finally|ON ERROR)"

# API endpoint changes
check_pattern "API Route / Endpoint Changes" \
    "^\+.*(Route|MapGet|MapPost|MapPut|MapDelete|HttpGet|HttpPost|HttpPut|HttpDelete|\[Api)"

# TODO / FIXME / HACK markers left in code
check_pattern "TODO / FIXME / HACK markers in new code" \
    "^\+.*(TODO|FIXME|HACK|XXX|WORKAROUND)"

if [[ "$flag_count" -eq 0 ]]; then
    append "  No automated red flags detected."
fi

# ─────────────────────────────────────────────────────────────────────────────
# 10. Commit Log (full)
# ─────────────────────────────────────────────────────────────────────────────
section "10. COMMIT LOG"

git log "$OLD_TAG".."$NEW_TAG" --format="  %h  %ai  %-20aN  %s" | while read -r line; do
    append "$line"
done

# ─────────────────────────────────────────────────────────────────────────────
# 11. Merge Commits (integration points)
# ─────────────────────────────────────────────────────────────────────────────
section "11. MERGE COMMITS"

MERGES=$(git log "$OLD_TAG".."$NEW_TAG" --merges --format="  %h  %ai  %s" 2>/dev/null || true)
if [[ -z "$MERGES" ]]; then
    append "  (no merge commits)"
else
    echo "$MERGES" | while read -r line; do
        append "$line"
    done
fi

# ─────────────────────────────────────────────────────────────────────────────
# Footer
# ─────────────────────────────────────────────────────────────────────────────
section "END OF REPORT"
append "  To see the full diff:  git diff $OLD_TAG..$NEW_TAG"
append "  To see diff for a specific file:  git diff $OLD_TAG..$NEW_TAG -- <filepath>"

# ─────────────────────────────────────────────────────────────────────────────
# Output
# ─────────────────────────────────────────────────────────────────────────────
echo "$REPORT"

if [[ -n "$OUTPUT_FILE" ]]; then
    echo "$REPORT" > "$OUTPUT_FILE"
    echo ""
    echo -e "${GREEN}Report saved to: $OUTPUT_FILE${NC}"
fi
