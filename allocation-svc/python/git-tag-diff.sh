#!/usr/bin/env bash
set -euo pipefail

# ─────────────────────────────────────────────────────────────────────────────
# git-tag-analysis.sh — Risk analysis & test impact for changes between tags
# ─────────────────────────────────────────────────────────────────────────────
# Usage:
#   ./git-tag-diff.sh <old_tag> <new_tag> [--repo /path/to/repo] [--output report.txt]
# ─────────────────────────────────────────────────────────────────────────────

REPO_PATH="."
OUTPUT_FILE=""

usage() {
    echo "Usage: $0 <old_tag> <new_tag> [--repo /path/to/repo] [--output report.txt]"
    exit 1
}

[[ $# -lt 2 ]] && usage

OLD_TAG="$1"; NEW_TAG="$2"; shift 2

while [[ $# -gt 0 ]]; do
    case "$1" in
        --repo)   REPO_PATH="$2"; shift 2 ;;
        --output) OUTPUT_FILE="$2"; shift 2 ;;
        *)        echo "Unknown option: $1"; usage ;;
    esac
done

[[ ! -d "$REPO_PATH/.git" ]] && echo "Error: '$REPO_PATH' is not a git repo." && exit 1
cd "$REPO_PATH"

for ref in "$OLD_TAG" "$NEW_TAG"; do
    git rev-parse "$ref" >/dev/null 2>&1 || { echo "Error: ref '$ref' not found."; exit 1; }
done

# ─────────────────────────────────────────────────────────────────────────────
# Temp workspace
# ─────────────────────────────────────────────────────────────────────────────
TMPDIR=$(mktemp -d)
trap 'rm -rf "$TMPDIR"' EXIT

FULL_DIFF="$TMPDIR/full.diff"
git diff "$OLD_TAG".."$NEW_TAG" > "$FULL_DIFF"

git diff --name-status "$OLD_TAG".."$NEW_TAG" > "$TMPDIR/name_status.txt"
git diff --numstat "$OLD_TAG".."$NEW_TAG"     > "$TMPDIR/numstat.txt"

# Separate file lists by change type
git diff --diff-filter=A --name-only "$OLD_TAG".."$NEW_TAG" > "$TMPDIR/added.txt"   2>/dev/null || true
git diff --diff-filter=M --name-only "$OLD_TAG".."$NEW_TAG" > "$TMPDIR/modified.txt" 2>/dev/null || true
git diff --diff-filter=D --name-only "$OLD_TAG".."$NEW_TAG" > "$TMPDIR/deleted.txt"  2>/dev/null || true

ALL_FILES="$TMPDIR/all_files.txt"
git diff --name-only "$OLD_TAG".."$NEW_TAG" > "$ALL_FILES"

# ─────────────────────────────────────────────────────────────────────────────
# Risk scoring accumulators
# ─────────────────────────────────────────────────────────────────────────────
declare -A RISK_FILES        # file -> risk level (HIGH/MEDIUM/LOW)
declare -A RISK_REASONS      # file -> reason string
declare -A FILE_TESTS        # file -> required test types
declare -A CATEGORY_FILES    # category -> file list

HIGH_RISK_COUNT=0
MEDIUM_RISK_COUNT=0
LOW_RISK_COUNT=0

REPORT=""
append()  { REPORT+="$1"$'\n'; }
section() { append ""; append "$(printf '━%.0s' {1..80})"; append "  $1"; append "$(printf '━%.0s' {1..80})"; append ""; }
warn()    { append "  ⚠  $1"; }
flag()    { append "  ✖  $1"; }
info()    { append "  ●  $1"; }
ok()      { append "  ✔  $1"; }

classify_file() {
    local file="$1"
    local risk="LOW"
    local reasons=""
    local tests=""

    local ext="${file##*.}"
    local base
    base=$(basename "$file")
    local dir
    dir=$(dirname "$file")
    local lower_file
    lower_file=$(echo "$file" | tr '[:upper:]' '[:lower:]')
    local lower_base
    lower_base=$(echo "$base" | tr '[:upper:]' '[:lower:]')

    local added=0 removed=0
    local stats
    stats=$(grep -E "	${file}$" "$TMPDIR/numstat.txt" 2>/dev/null | head -1 || true)
    if [[ -n "$stats" ]]; then
        added=$(echo "$stats" | awk '{print $1}')
        removed=$(echo "$stats" | awk '{print $2}')
        [[ "$added" == "-" ]] && added=0
        [[ "$removed" == "-" ]] && removed=0
    fi
    local churn=$((added + removed))

    local is_deleted=false
    grep -qxF "$file" "$TMPDIR/deleted.txt" 2>/dev/null && is_deleted=true

    local is_added=false
    grep -qxF "$file" "$TMPDIR/added.txt" 2>/dev/null && is_added=true

    # Extract the diff chunk for this file
    local file_diff
    file_diff=$(git diff "$OLD_TAG".."$NEW_TAG" -- "$file" 2>/dev/null || true)
    local added_lines
    added_lines=$(echo "$file_diff" | grep -E '^\+[^+]' 2>/dev/null || true)

    # ── CATEGORY: Database / Schema ──────────────────────────────────
    if echo "$lower_file" | grep -qE '(migration|schema|\.sql$|flyway|liquibase|dacpac)'; then
        risk="HIGH"
        reasons+="Database schema/migration file. "
        tests+="DB_MIGRATION,ROLLBACK_TEST,INTEGRATION,"
        CATEGORY_FILES["DATABASE"]+="$file|"
    fi

    if echo "$added_lines" | grep -qiE '(alter table|drop table|create table|add column|drop column|create index|drop index|truncate|rename table)'; then
        risk="HIGH"
        reasons+="Contains DDL statements (schema changes). "
        tests+="DB_MIGRATION,ROLLBACK_TEST,DATA_INTEGRITY,"
        CATEGORY_FILES["DATABASE"]+="$file|"
    fi

    if echo "$added_lines" | grep -qiE '(create proc|alter proc|create function|alter function|create trigger|drop proc|drop function|drop trigger)'; then
        risk="HIGH"
        reasons+="Stored procedure/function/trigger changes. "
        tests+="STORED_PROC_UNIT,INTEGRATION,REGRESSION,"
        CATEGORY_FILES["DATABASE"]+="$file|"
    fi

    # ── CATEGORY: Security ───────────────────────────────────────────
    if echo "$added_lines" | grep -qiE '(password|secret|token|api.?key|credential|private.?key|encrypt|decrypt|auth|oauth|jwt|certificate|ssl|tls)'; then
        [[ "$risk" != "HIGH" ]] && risk="HIGH"
        reasons+="Security-sensitive changes detected. "
        tests+="SECURITY_AUDIT,PENETRATION,AUTH_TEST,"
        CATEGORY_FILES["SECURITY"]+="$file|"
    fi

    if echo "$lower_file" | grep -qE '(auth|security|login|permission|role|access|crypto|certificate)'; then
        [[ "$risk" != "HIGH" ]] && risk="HIGH"
        reasons+="Security-related file. "
        tests+="SECURITY_AUDIT,AUTH_TEST,ROLE_TEST,"
        CATEGORY_FILES["SECURITY"]+="$file|"
    fi

    # ── CATEGORY: API Contract ───────────────────────────────────────
    if echo "$added_lines" | grep -qiE '(Route|MapGet|MapPost|MapPut|MapDelete|HttpGet|HttpPost|HttpPut|HttpDelete|\[Api|WebMethod|endpoint|@RequestMapping|@GetMapping|@PostMapping)'; then
        [[ "$risk" != "HIGH" ]] && risk="HIGH"
        reasons+="API endpoint/contract changes. "
        tests+="API_CONTRACT,CONSUMER_TEST,INTEGRATION,"
        CATEGORY_FILES["API"]+="$file|"
    fi

    if echo "$lower_file" | grep -qE '(controller|api|endpoint|handler|route|swagger|openapi)'; then
        [[ "$risk" == "LOW" ]] && risk="MEDIUM"
        reasons+="API layer file. "
        tests+="API_CONTRACT,INTEGRATION,"
        CATEGORY_FILES["API"]+="$file|"
    fi

    # ── CATEGORY: Configuration ──────────────────────────────────────
    if echo "$lower_base" | grep -qE '(\.config$|\.yml$|\.yaml$|\.env|appsettings|web\.config|app\.config|\.properties$|\.ini$|\.toml$)'; then
        [[ "$risk" == "LOW" ]] && risk="MEDIUM"
        reasons+="Configuration file changed. "
        tests+="ENVIRONMENT_VALIDATION,SMOKE_TEST,"
        CATEGORY_FILES["CONFIG"]+="$file|"
    fi

    if echo "$added_lines" | grep -qiE '(connection.?string|server=|data source|initial catalog|database=|host=|port=)'; then
        risk="HIGH"
        reasons+="Connection/infrastructure config changed. "
        tests+="ENVIRONMENT_VALIDATION,CONNECTIVITY_TEST,SMOKE_TEST,"
        CATEGORY_FILES["CONFIG"]+="$file|"
    fi

    # ── CATEGORY: Dependencies ───────────────────────────────────────
    if echo "$lower_base" | grep -qE '(package\.json|requirements\.txt|pom\.xml|\.csproj|packages\.config|gemfile|cargo\.toml|go\.mod|go\.sum|yarn\.lock|package-lock|nuget)'; then
        [[ "$risk" == "LOW" ]] && risk="MEDIUM"
        reasons+="Dependency changes. "
        tests+="DEPENDENCY_AUDIT,BUILD_VERIFY,REGRESSION,"
        CATEGORY_FILES["DEPENDENCY"]+="$file|"
    fi

    # ── CATEGORY: Core Business Logic ────────────────────────────────
    if echo "$lower_file" | grep -qE '(service|engine|processor|calculator|validator|manager|handler|core|domain|business|workflow|rule)'; then
        if [[ "$churn" -gt 50 ]]; then
            [[ "$risk" != "HIGH" ]] && risk="HIGH"
            reasons+="Core business logic with heavy changes ($churn lines). "
            tests+="UNIT,REGRESSION,INTEGRATION,"
        elif [[ "$churn" -gt 0 ]]; then
            [[ "$risk" == "LOW" ]] && risk="MEDIUM"
            reasons+="Core business logic modified. "
            tests+="UNIT,REGRESSION,"
        fi
        CATEGORY_FILES["BUSINESS_LOGIC"]+="$file|"
    fi

    # ── CATEGORY: Data Access / ORM ──────────────────────────────────
    if echo "$lower_file" | grep -qE '(repository|dao|dal|dataaccess|dbcontext|entity|model|mapping|orm)'; then
        [[ "$risk" == "LOW" ]] && risk="MEDIUM"
        reasons+="Data access layer changed. "
        tests+="INTEGRATION,DATA_INTEGRITY,REGRESSION,"
        CATEGORY_FILES["DATA_ACCESS"]+="$file|"
    fi

    # ── CATEGORY: Shared/Common Libraries ────────────────────────────
    if echo "$lower_file" | grep -qE '(common|shared|util|helper|lib|framework|base|abstract|interface|extension)'; then
        if [[ "$churn" -gt 20 ]]; then
            [[ "$risk" != "HIGH" ]] && risk="HIGH"
            reasons+="Shared library with broad impact ($churn lines changed). "
        else
            [[ "$risk" == "LOW" ]] && risk="MEDIUM"
            reasons+="Shared/common library modified. "
        fi
        tests+="UNIT,REGRESSION,INTEGRATION,"
        CATEGORY_FILES["SHARED_LIB"]+="$file|"
    fi

    # ── CATEGORY: Concurrency / Threading ────────────────────────────
    if echo "$added_lines" | grep -qiE '(lock|mutex|semaphore|thread|async|await|Task\.Run|parallel|concurrent|Interlocked|volatile|Monitor\.|deadlock)'; then
        risk="HIGH"
        reasons+="Concurrency/threading changes. "
        tests+="CONCURRENCY_TEST,LOAD_TEST,STRESS_TEST,"
        CATEGORY_FILES["CONCURRENCY"]+="$file|"
    fi

    # ── CATEGORY: Error Handling ─────────────────────────────────────
    if echo "$added_lines" | grep -qiE '(try|catch|throw|finally|except|raise|on error|error.?handler)'; then
        local error_churn
        error_churn=$(echo "$added_lines" | grep -ciE '(try|catch|throw|finally|except)' 2>/dev/null || echo 0)
        if [[ "$error_churn" -gt 5 ]]; then
            [[ "$risk" == "LOW" ]] && risk="MEDIUM"
            reasons+="Significant error handling changes ($error_churn instances). "
            tests+="ERROR_SCENARIO,NEGATIVE_TEST,"
            CATEGORY_FILES["ERROR_HANDLING"]+="$file|"
        fi
    fi

    # ── CATEGORY: Caching / Performance ──────────────────────────────
    if echo "$added_lines" | grep -qiE '(cache|redis|memcache|invalidat|ttl|expir|lazy.?load|eager.?load|prefetch|batch.?size|buffer)'; then
        [[ "$risk" == "LOW" ]] && risk="MEDIUM"
        reasons+="Caching/performance changes. "
        tests+="PERFORMANCE,LOAD_TEST,CACHE_VALIDATION,"
        CATEGORY_FILES["PERFORMANCE"]+="$file|"
    fi

    # ── CATEGORY: Messaging / Queue ──────────────────────────────────
    if echo "$added_lines" | grep -qiE '(queue|topic|publish|subscribe|kafka|rabbitmq|service.?bus|event.?hub|message.?broker|amqp)'; then
        risk="HIGH"
        reasons+="Messaging/queue changes. "
        tests+="INTEGRATION,MESSAGE_FLOW,END_TO_END,"
        CATEGORY_FILES["MESSAGING"]+="$file|"
    fi

    # ── CATEGORY: Scheduling / Jobs ──────────────────────────────────
    if echo "$lower_file" | grep -qiE '(job|scheduler|cron|worker|batch|hangfire|quartz)'; then
        [[ "$risk" == "LOW" ]] && risk="MEDIUM"
        reasons+="Scheduled job/batch process changed. "
        tests+="JOB_EXECUTION,REGRESSION,SMOKE_TEST,"
        CATEGORY_FILES["JOBS"]+="$file|"
    fi

    # ── CATEGORY: Tests ──────────────────────────────────────────────
    if echo "$lower_file" | grep -qE '(test|spec|\.test\.|\.spec\.|_test\.|tests/)'; then
        risk="LOW"
        reasons+="Test file. "
        tests+="VERIFY_TESTS_PASS,"
        CATEGORY_FILES["TESTS"]+="$file|"
    fi

    # ── CATEGORY: Documentation ──────────────────────────────────────
    if echo "$lower_base" | grep -qE '(\.md$|\.txt$|\.rst$|readme|changelog|license|\.adoc$)'; then
        risk="LOW"
        reasons+="Documentation. "
        tests+="NONE,"
        CATEGORY_FILES["DOCS"]+="$file|"
    fi

    # ── CATEGORY: Build / CI/CD ──────────────────────────────────────
    if echo "$lower_file" | grep -qE '(dockerfile|docker-compose|\.github|jenkins|pipeline|ci|cd|deploy|terraform|ansible|helm|\.tf$|makefile|build\.gradle|build\.xml)'; then
        [[ "$risk" == "LOW" ]] && risk="MEDIUM"
        reasons+="Build/CI-CD/Infrastructure changed. "
        tests+="BUILD_VERIFY,DEPLOY_TEST,SMOKE_TEST,"
        CATEGORY_FILES["CICD"]+="$file|"
    fi

    # ── DELETION risk boost ──────────────────────────────────────────
    if [[ "$is_deleted" == true ]]; then
        [[ "$risk" == "LOW" ]] && risk="MEDIUM"
        reasons+="FILE DELETED — check all consumers. "
        tests+="REGRESSION,INTEGRATION,"
    fi

    # ── High churn catch-all ─────────────────────────────────────────
    if [[ "$churn" -gt 200 && "$risk" == "LOW" ]]; then
        risk="MEDIUM"
        reasons+="Large change volume ($churn lines). "
        tests+="REGRESSION,"
    fi

    # ── Default for unclassified ─────────────────────────────────────
    [[ -z "$reasons" ]] && reasons="General code change. " && tests+="UNIT,"

    RISK_FILES["$file"]="$risk"
    RISK_REASONS["$file"]="$reasons"
    FILE_TESTS["$file"]="$tests"

    case "$risk" in
        HIGH)   HIGH_RISK_COUNT=$((HIGH_RISK_COUNT + 1)) ;;
        MEDIUM) MEDIUM_RISK_COUNT=$((MEDIUM_RISK_COUNT + 1)) ;;
        LOW)    LOW_RISK_COUNT=$((LOW_RISK_COUNT + 1)) ;;
    esac
}

# ─────────────────────────────────────────────────────────────────────────────
# Classify every changed file
# ─────────────────────────────────────────────────────────────────────────────
while IFS= read -r file; do
    [[ -z "$file" ]] && continue
    classify_file "$file"
done < "$ALL_FILES"

TOTAL_FILES=$((HIGH_RISK_COUNT + MEDIUM_RISK_COUNT + LOW_RISK_COUNT))
COMMIT_COUNT=$(git rev-list --count "$OLD_TAG".."$NEW_TAG")

# ═════════════════════════════════════════════════════════════════════════════
# BUILD THE REPORT
# ═════════════════════════════════════════════════════════════════════════════

section "RELEASE RISK ANALYSIS: $OLD_TAG → $NEW_TAG"
append "  Generated : $(date '+%Y-%m-%d %H:%M:%S')"
append "  Repository: $(basename "$(git rev-parse --show-toplevel)")"
append "  Commits   : $COMMIT_COUNT"
append "  Files     : $TOTAL_FILES"

# ─────────────────────────────────────────────────────────────────────────────
# OVERALL RISK SCORE
# ─────────────────────────────────────────────────────────────────────────────
section "OVERALL RISK ASSESSMENT"

RISK_SCORE=$((HIGH_RISK_COUNT * 10 + MEDIUM_RISK_COUNT * 3 + LOW_RISK_COUNT * 1))

if [[ "$HIGH_RISK_COUNT" -ge 5 || "$RISK_SCORE" -ge 80 ]]; then
    OVERALL_RISK="CRITICAL"
    append "  ╔══════════════════════════════════════════════════════╗"
    append "  ║  OVERALL RISK:  ✖ CRITICAL  (score: $RISK_SCORE)              ║"
    append "  ║  Recommendation: Full regression + staged rollout   ║"
    append "  ╚══════════════════════════════════════════════════════╝"
elif [[ "$HIGH_RISK_COUNT" -ge 2 || "$RISK_SCORE" -ge 40 ]]; then
    OVERALL_RISK="HIGH"
    append "  ╔══════════════════════════════════════════════════════╗"
    append "  ║  OVERALL RISK:  ✖ HIGH  (score: $RISK_SCORE)                  ║"
    append "  ║  Recommendation: Targeted regression + review       ║"
    append "  ╚══════════════════════════════════════════════════════╝"
elif [[ "$MEDIUM_RISK_COUNT" -ge 3 || "$RISK_SCORE" -ge 15 ]]; then
    OVERALL_RISK="MEDIUM"
    append "  ╔══════════════════════════════════════════════════════╗"
    append "  ║  OVERALL RISK:  ⚠ MEDIUM  (score: $RISK_SCORE)                ║"
    append "  ║  Recommendation: Focused testing on flagged areas   ║"
    append "  ╚══════════════════════════════════════════════════════╝"
else
    OVERALL_RISK="LOW"
    append "  ╔══════════════════════════════════════════════════════╗"
    append "  ║  OVERALL RISK:  ✔ LOW  (score: $RISK_SCORE)                   ║"
    append "  ║  Recommendation: Smoke test + spot checks           ║"
    append "  ╚══════════════════════════════════════════════════════╝"
fi

append ""
append "  Risk Breakdown:"
append "    HIGH   files: $HIGH_RISK_COUNT"
append "    MEDIUM files: $MEDIUM_RISK_COUNT"
append "    LOW    files: $LOW_RISK_COUNT"

# ─────────────────────────────────────────────────────────────────────────────
# HIGH RISK FILES — DETAILED
# ─────────────────────────────────────────────────────────────────────────────
section "HIGH RISK FILES — MUST REVIEW"

if [[ "$HIGH_RISK_COUNT" -eq 0 ]]; then
    ok "No high-risk files detected."
else
    append "  These files carry the highest risk and MUST be reviewed before release."
    append ""
    for file in "${!RISK_FILES[@]}"; do
        if [[ "${RISK_FILES[$file]}" == "HIGH" ]]; then
            flag "$file"
            append "       Risk    : ${RISK_REASONS[$file]}"
            # Deduplicate tests
            local_tests=$(echo "${FILE_TESTS[$file]}" | tr ',' '\n' | sort -u | grep -v '^$' | tr '\n' ',' | sed 's/,$//')
            append "       Tests   : $local_tests"
            append ""
        fi
    done
fi

# ─────────────────────────────────────────────────────────────────────────────
# MEDIUM RISK FILES
# ─────────────────────────────────────────────────────────────────────────────
section "MEDIUM RISK FILES — SHOULD REVIEW"

if [[ "$MEDIUM_RISK_COUNT" -eq 0 ]]; then
    ok "No medium-risk files detected."
else
    for file in "${!RISK_FILES[@]}"; do
        if [[ "${RISK_FILES[$file]}" == "MEDIUM" ]]; then
            warn "$file"
            append "       Reason: ${RISK_REASONS[$file]}"
            local_tests=$(echo "${FILE_TESTS[$file]}" | tr ',' '\n' | sort -u | grep -v '^$' | tr '\n' ',' | sed 's/,$//')
            append "       Tests : $local_tests"
            append ""
        fi
    done
fi

# ─────────────────────────────────────────────────────────────────────────────
# LOW RISK FILES (summary only)
# ─────────────────────────────────────────────────────────────────────────────
section "LOW RISK FILES — SPOT CHECK"

if [[ "$LOW_RISK_COUNT" -eq 0 ]]; then
    ok "No low-risk files."
else
    for file in "${!RISK_FILES[@]}"; do
        if [[ "${RISK_FILES[$file]}" == "LOW" ]]; then
            info "$file  —  ${RISK_REASONS[$file]}"
        fi
    done
fi

# ─────────────────────────────────────────────────────────────────────────────
# IMPACT ANALYSIS BY CATEGORY
# ─────────────────────────────────────────────────────────────────────────────
section "IMPACT ANALYSIS BY CHANGE CATEGORY"

print_category() {
    local cat_name="$1"
    local display_name="$2"
    local impact_desc="$3"
    local raw="${CATEGORY_FILES[$cat_name]:-}"
    [[ -z "$raw" ]] && return

    # Deduplicate
    local files
    files=$(echo "$raw" | tr '|' '\n' | sort -u | grep -v '^$')
    local count
    count=$(echo "$files" | wc -l | tr -d ' ')

    append "  [$display_name] — $count file(s)"
    append "    Impact: $impact_desc"
    echo "$files" | while IFS= read -r f; do
        [[ -z "$f" ]] && continue
        append "      • $f"
    done
    append ""
}

print_category "DATABASE"       "Database / Schema"    "Data integrity, dependent queries, stored procs, downstream ETL, reporting"
print_category "SECURITY"       "Security"             "Authentication, authorization, data exposure, compliance"
print_category "API"            "API / Endpoints"      "Consumer applications, mobile clients, third-party integrations"
print_category "CONFIG"         "Configuration"        "All environments (dev/staging/prod), deployment pipelines"
print_category "BUSINESS_LOGIC" "Business Logic"       "Core calculations, validation rules, workflow behavior"
print_category "DATA_ACCESS"    "Data Access Layer"    "Query performance, data mapping, transaction boundaries"
print_category "SHARED_LIB"    "Shared Libraries"     "ALL consumers of these libraries — broad blast radius"
print_category "CONCURRENCY"    "Concurrency"          "Race conditions, deadlocks, data corruption under load"
print_category "MESSAGING"      "Messaging / Queues"   "Message producers, consumers, event processing order"
print_category "JOBS"           "Jobs / Schedulers"    "Batch processing, scheduled tasks, data pipelines"
print_category "DEPENDENCY"     "Dependencies"         "Build stability, transitive vulnerabilities, breaking upgrades"
print_category "CICD"           "Build / CI-CD"        "Build pipelines, deployment process, infrastructure"
print_category "PERFORMANCE"    "Caching/Performance"  "Response times, cache coherence, memory usage"
print_category "ERROR_HANDLING" "Error Handling"       "Failure modes, retry behavior, user-facing error messages"
print_category "TESTS"          "Test Code"            "Test coverage, test reliability"
print_category "DOCS"           "Documentation"        "Low impact — informational only"

# ─────────────────────────────────────────────────────────────────────────────
# REQUIRED TEST PLAN
# ─────────────────────────────────────────────────────────────────────────────
section "REQUIRED TEST PLAN"

append "  Based on the changes detected, the following test types are required:"
append ""

# Aggregate all required tests
ALL_TESTS=""
for file in "${!FILE_TESTS[@]}"; do
    ALL_TESTS+="${FILE_TESTS[$file]}"
done

declare -A TEST_DESCRIPTIONS
TEST_DESCRIPTIONS["DB_MIGRATION"]="Run migration scripts forward AND backward in a staging environment"
TEST_DESCRIPTIONS["ROLLBACK_TEST"]="Verify the release can be rolled back without data loss"
TEST_DESCRIPTIONS["DATA_INTEGRITY"]="Validate data consistency before/after migration (row counts, checksums)"
TEST_DESCRIPTIONS["STORED_PROC_UNIT"]="Execute stored procedure unit tests with edge-case inputs"
TEST_DESCRIPTIONS["INTEGRATION"]="Run integration test suite across affected service boundaries"
TEST_DESCRIPTIONS["REGRESSION"]="Full regression test suite for affected modules"
TEST_DESCRIPTIONS["UNIT"]="Run unit tests for modified business logic"
TEST_DESCRIPTIONS["SECURITY_AUDIT"]="Review security changes for vulnerabilities (OWASP checklist)"
TEST_DESCRIPTIONS["PENETRATION"]="Run automated security scans (SAST/DAST) on changed endpoints"
TEST_DESCRIPTIONS["AUTH_TEST"]="Verify authentication flows (login, token refresh, session expiry)"
TEST_DESCRIPTIONS["ROLE_TEST"]="Test role-based access control for all permission levels"
TEST_DESCRIPTIONS["API_CONTRACT"]="Validate API request/response schemas haven't broken consumers"
TEST_DESCRIPTIONS["CONSUMER_TEST"]="Verify downstream API consumers still function correctly"
TEST_DESCRIPTIONS["ENVIRONMENT_VALIDATION"]="Validate config changes across all target environments"
TEST_DESCRIPTIONS["SMOKE_TEST"]="Post-deployment smoke test on critical paths"
TEST_DESCRIPTIONS["CONNECTIVITY_TEST"]="Verify database/service connectivity in target environment"
TEST_DESCRIPTIONS["DEPENDENCY_AUDIT"]="Check new/updated dependencies for known vulnerabilities (CVE scan)"
TEST_DESCRIPTIONS["BUILD_VERIFY"]="Verify clean build from scratch with new dependencies"
TEST_DESCRIPTIONS["PERFORMANCE"]="Run performance benchmarks and compare to baseline"
TEST_DESCRIPTIONS["LOAD_TEST"]="Load test affected endpoints under expected peak traffic"
TEST_DESCRIPTIONS["STRESS_TEST"]="Stress test concurrent operations for deadlocks/race conditions"
TEST_DESCRIPTIONS["CONCURRENCY_TEST"]="Test thread safety with concurrent requests to shared resources"
TEST_DESCRIPTIONS["CACHE_VALIDATION"]="Verify cache invalidation works correctly after data changes"
TEST_DESCRIPTIONS["MESSAGE_FLOW"]="Validate end-to-end message flow through queues/topics"
TEST_DESCRIPTIONS["END_TO_END"]="Full end-to-end workflow test across all affected systems"
TEST_DESCRIPTIONS["JOB_EXECUTION"]="Test scheduled job execution with expected and edge-case data"
TEST_DESCRIPTIONS["ERROR_SCENARIO"]="Test failure scenarios (network down, timeout, invalid data)"
TEST_DESCRIPTIONS["NEGATIVE_TEST"]="Test with invalid inputs, boundary values, and error conditions"
TEST_DESCRIPTIONS["DEPLOY_TEST"]="Test deployment script in staging before production"
TEST_DESCRIPTIONS["VERIFY_TESTS_PASS"]="Ensure all modified/new tests pass in CI"
TEST_DESCRIPTIONS["NONE"]=""

# Deduplicate and sort tests by priority
PRIORITY_ORDER="DB_MIGRATION ROLLBACK_TEST DATA_INTEGRITY SECURITY_AUDIT PENETRATION AUTH_TEST ROLE_TEST API_CONTRACT CONSUMER_TEST STORED_PROC_UNIT CONCURRENCY_TEST STRESS_TEST LOAD_TEST INTEGRATION END_TO_END MESSAGE_FLOW REGRESSION UNIT ERROR_SCENARIO NEGATIVE_TEST PERFORMANCE CACHE_VALIDATION JOB_EXECUTION ENVIRONMENT_VALIDATION CONNECTIVITY_TEST DEPENDENCY_AUDIT BUILD_VERIFY DEPLOY_TEST SMOKE_TEST VERIFY_TESTS_PASS"

priority_num=1
for test_type in $PRIORITY_ORDER; do
    if echo "$ALL_TESTS" | grep -q "$test_type"; then
        desc="${TEST_DESCRIPTIONS[$test_type]:-}"
        if [[ -n "$desc" ]]; then
            # Find which files require this test
            requiring_files=""
            for file in "${!FILE_TESTS[@]}"; do
                if echo "${FILE_TESTS[$file]}" | grep -q "$test_type"; then
                    requiring_files+="$file, "
                fi
            done
            requiring_files=$(echo "$requiring_files" | sed 's/, $//')

            printf -v padded_num "%2d" "$priority_num"
            append "  $padded_num. [$test_type]"
            append "      Action : $desc"
            append "      Trigger: $requiring_files"
            append ""
            priority_num=$((priority_num + 1))
        fi
    fi
done

# ─────────────────────────────────────────────────────────────────────────────
# BLAST RADIUS — who should be notified
# ─────────────────────────────────────────────────────────────────────────────
section "BLAST RADIUS — STAKEHOLDER NOTIFICATIONS"

[[ -n "${CATEGORY_FILES[DATABASE]:-}" ]]       && warn "DBA Team — Database schema/stored procedure changes detected"
[[ -n "${CATEGORY_FILES[SECURITY]:-}" ]]       && warn "Security Team — Security-sensitive code modified"
[[ -n "${CATEGORY_FILES[API]:-}" ]]            && warn "API Consumers — Endpoint changes may affect downstream clients"
[[ -n "${CATEGORY_FILES[CONFIG]:-}" ]]         && warn "DevOps/Infra — Configuration changes require environment review"
[[ -n "${CATEGORY_FILES[MESSAGING]:-}" ]]      && warn "Integration Team — Messaging/queue changes detected"
[[ -n "${CATEGORY_FILES[CICD]:-}" ]]           && warn "DevOps — Build/deployment pipeline modified"
[[ -n "${CATEGORY_FILES[SHARED_LIB]:-}" ]]     && warn "All Teams — Shared library changes have broad impact"
[[ -n "${CATEGORY_FILES[BUSINESS_LOGIC]:-}" ]] && warn "Business/QA — Core business logic modified"
[[ -n "${CATEGORY_FILES[JOBS]:-}" ]]           && warn "Operations — Scheduled job/batch process changes"
[[ -n "${CATEGORY_FILES[PERFORMANCE]:-}" ]]    && warn "Performance Team — Caching/performance changes require benchmarking"

# ─────────────────────────────────────────────────────────────────────────────
# UNFINISHED WORK MARKERS
# ─────────────────────────────────────────────────────────────────────────────
section "CODE QUALITY FLAGS"

TODO_MATCHES=$(grep -rn '^\+.*\(TODO\|FIXME\|HACK\|XXX\|WORKAROUND\|TEMP\|KLUDGE\)' "$FULL_DIFF" 2>/dev/null || true)
if [[ -n "$TODO_MATCHES" ]]; then
    flag "Unfinished work markers found in NEW code:"
    append ""
    echo "$TODO_MATCHES" | head -20 | while IFS= read -r line; do
        append "    $line"
    done
    MARKER_COUNT=$(echo "$TODO_MATCHES" | wc -l | tr -d ' ')
    [[ "$MARKER_COUNT" -gt 20 ]] && append "    ... and $((MARKER_COUNT - 20)) more"
else
    ok "No TODO/FIXME/HACK markers in new code."
fi

append ""

LARGE_FILE_CHANGES=$(awk '$1 != "-" && $2 != "-" { total=$1+$2; if (total > 500) printf "    %s (%d lines changed)\n", $3, total }' "$TMPDIR/numstat.txt")
if [[ -n "$LARGE_FILE_CHANGES" ]]; then
    warn "Unusually large file changes (>500 lines) — consider splitting:"
    append ""
    echo "$LARGE_FILE_CHANGES" | while IFS= read -r line; do
        append "$line"
    done
else
    ok "No unusually large single-file changes."
fi

# ─────────────────────────────────────────────────────────────────────────────
# COMMIT ANALYSIS — Patterns
# ─────────────────────────────────────────────────────────────────────────────
section "COMMIT PATTERN ANALYSIS"

REVERT_COUNT=$(git log "$OLD_TAG".."$NEW_TAG" --format='%s' | grep -ciE '^revert' || echo 0)
FIXUP_COUNT=$(git log "$OLD_TAG".."$NEW_TAG" --format='%s' | grep -ciE '(fixup|fix up|hotfix|quick fix|band.?aid)' || echo 0)
WIP_COUNT=$(git log "$OLD_TAG".."$NEW_TAG" --format='%s' | grep -ciE '^(wip|work in progress)' || echo 0)
MERGE_COUNT=$(git log "$OLD_TAG".."$NEW_TAG" --merges --format='%h' | wc -l | tr -d ' ')

[[ "$REVERT_COUNT" -gt 0 ]] && warn "Reverted commits: $REVERT_COUNT — indicates instability in this release"
[[ "$FIXUP_COUNT" -gt 0 ]]  && warn "Fixup/hotfix commits: $FIXUP_COUNT — late-stage patches detected"
[[ "$WIP_COUNT" -gt 0 ]]    && flag "WIP commits: $WIP_COUNT — unfinished work may have been merged"
[[ "$MERGE_COUNT" -gt 0 ]]  && info "Merge commits: $MERGE_COUNT"

if [[ "$REVERT_COUNT" -eq 0 && "$FIXUP_COUNT" -eq 0 && "$WIP_COUNT" -eq 0 ]]; then
    ok "Commit history looks clean — no reverts, fixups, or WIP markers."
fi

# ─────────────────────────────────────────────────────────────────────────────
# EXECUTIVE SUMMARY
# ─────────────────────────────────────────────────────────────────────────────
section "EXECUTIVE SUMMARY"

append "  Release        : $OLD_TAG → $NEW_TAG"
append "  Overall Risk   : $OVERALL_RISK (score: $RISK_SCORE)"
append "  Total Changes  : $TOTAL_FILES files across $COMMIT_COUNT commits"
append "  High Risk      : $HIGH_RISK_COUNT files requiring mandatory review"
append "  Medium Risk    : $MEDIUM_RISK_COUNT files requiring targeted review"
append "  Low Risk       : $LOW_RISK_COUNT files (spot check)"
append ""

test_count=$(echo "$ALL_TESTS" | tr ',' '\n' | sort -u | grep -cv '^$' || echo 0)
append "  Required Tests : $test_count distinct test types identified"
append ""

if [[ "$OVERALL_RISK" == "CRITICAL" || "$OVERALL_RISK" == "HIGH" ]]; then
    append "  RECOMMENDATION:"
    append "    1. Mandatory code review for all HIGH risk files"
    append "    2. Execute full test plan above before release"
    append "    3. Stage rollout (canary/blue-green) — do NOT deploy all-at-once"
    append "    4. Have rollback plan ready and tested"
    append "    5. Monitor closely for 24-48 hours post-deployment"
elif [[ "$OVERALL_RISK" == "MEDIUM" ]]; then
    append "  RECOMMENDATION:"
    append "    1. Review HIGH and MEDIUM risk files"
    append "    2. Execute targeted tests from the test plan"
    append "    3. Standard deployment with monitoring"
    append "    4. Rollback plan should be documented"
else
    append "  RECOMMENDATION:"
    append "    1. Spot-check changes"
    append "    2. Run smoke tests post-deployment"
    append "    3. Standard deployment process"
fi

append ""
append "$(printf '━%.0s' {1..80})"
append "  END OF ANALYSIS"
append "$(printf '━%.0s' {1..80})"

# ─────────────────────────────────────────────────────────────────────────────
# Output
# ─────────────────────────────────────────────────────────────────────────────
echo "$REPORT"

if [[ -n "$OUTPUT_FILE" ]]; then
    echo "$REPORT" > "$OUTPUT_FILE"
    echo ""
    echo "Report saved to: $OUTPUT_FILE"
fi
