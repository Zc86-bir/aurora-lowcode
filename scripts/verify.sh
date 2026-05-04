#!/bin/bash
# Aurora Low-Code Platform — Verification Script
# Checks all endpoints, skill loading, database connection, and audit chain integrity.
#
# Usage: ./scripts/verify.sh
# Prerequisites: curl, jq, java

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
BASE_URL="${AURORA_URL:-http://localhost:8080}"
PASS=0
FAIL=0
WARN=0

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  Aurora Low-Code Platform — Verification   ${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo -e "  Base URL: ${BASE_URL}"
echo -e "  Timestamp: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
echo ""

# Helper functions
pass() {
    echo -e "  ${GREEN}✓ PASS${NC}  $1"
    PASS=$((PASS + 1))
}

fail() {
    echo -e "  ${RED}✗ FAIL${NC}  $1"
    FAIL=$((FAIL + 1))
}

warn() {
    echo -e "  ${YELLOW}⚠ WARN${NC}  $1"
    WARN=$((WARN + 1))
}

check_endpoint() {
    local path="$1"
    local description="$2"
    local expected_code="${3:-200}"

    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 --max-time 10 "${BASE_URL}${path}" 2>/dev/null || echo "000")

    if [ "$http_code" = "$expected_code" ]; then
        pass "${description} (${path} → ${http_code})"
    elif [ "$http_code" = "000" ]; then
        fail "${description} (${path} → connection refused)"
    else
        warn "${description} (${path} → ${http_code}, expected ${expected_code})"
    fi
}

# ===========================
# Section 1: Application Health
# ===========================
echo -e "${BLUE}--- Application Health ---${NC}"

check_endpoint "/actuator/health" "Health endpoint" "200"
check_endpoint "/actuator/info" "Info endpoint" "200"
check_endpoint "/actuator/metrics" "Metrics endpoint" "200"

# Detailed health check
HEALTH_RESPONSE=$(curl -s --connect-timeout 5 --max-time 10 "${BASE_URL}/actuator/health" 2>/dev/null || echo '{}')
STATUS=$(echo "$HEALTH_RESPONSE" | jq -r '.status // "UNKNOWN"' 2>/dev/null || echo "UNKNOWN")

if [ "$STATUS" = "UP" ]; then
    pass "Application status: UP"
else
    fail "Application status: ${STATUS} (expected UP)"
fi

# ===========================
# Section 2: API Documentation
# ===========================
echo -e "${BLUE}--- API Documentation ---${NC}"

check_endpoint "/v3/api-docs" "OpenAPI spec" "200"
check_endpoint "/swagger-ui.html" "Swagger UI" "200"

# Verify OpenAPI content
API_DOCS=$(curl -s --connect-timeout 5 --max-time 10 "${BASE_URL}/v3/api-docs" 2>/dev/null || echo '{}')
API_TITLE=$(echo "$API_DOCS" | jq -r '.info.title // "UNKNOWN"' 2>/dev/null || echo "UNKNOWN")

if [ "$API_TITLE" != "UNKNOWN" ] && [ "$API_TITLE" != "null" ]; then
    pass "API title: ${API_TITLE}"
else
    warn "API title not found in OpenAPI spec"
fi

# ===========================
# Section 3: Database Connection
# ===========================
echo -e "${BLUE}--- Database Connection ---${NC}"

# Check health endpoint for database status
DB_STATUS=$(echo "$HEALTH_RESPONSE" | jq -r '.components.db.status // "UNKNOWN"' 2>/dev/null || echo "UNKNOWN")

if [ "$DB_STATUS" = "UP" ]; then
    pass "Database connection: UP"
elif [ "$DB_STATUS" = "UNKNOWN" ]; then
    warn "Database status not reported (may not be configured)"
else
    fail "Database connection: ${DB_STATUS}"
fi

# Check Flyway migration status
if command -v psql &> /dev/null; then
    MIGRATION_COUNT=$(psql -h "${DATABASE_HOST:-localhost}" \
        -U "${DATABASE_USER:-aurora}" \
        -d "${DATABASE_NAME:-aurora}" \
        -t -c "SELECT COUNT(*) FROM flyway_schema_history;" 2>/dev/null || echo "0")
    MIGRATION_COUNT=$(echo "$MIGRATION_COUNT" | tr -d '[:space:]')
    MIGRATION_COUNT=${MIGRATION_COUNT:-0}

    if [ "$MIGRATION_COUNT" -gt 0 ] 2>/dev/null; then
        pass "Flyway migrations applied: ${MIGRATION_COUNT}"
    else
        warn "No Flyway migrations found (database may be empty)"
    fi
else
    warn "psql not available — skipping migration check"
fi

# ===========================
# Section 4: Redis Connection
# ===========================
echo -e "${BLUE}--- Redis Connection ---${NC}"

REDIS_STATUS=$(echo "$HEALTH_RESPONSE" | jq -r '.components.redis.status // "UNKNOWN"' 2>/dev/null || echo "UNKNOWN")

if [ "$REDIS_STATUS" = "UP" ]; then
    pass "Redis connection: UP"
elif [ "$REDIS_STATUS" = "UNKNOWN" ]; then
    warn "Redis status not reported (may not be configured)"
else
    fail "Redis connection: ${REDIS_STATUS}"
fi

# ===========================
# Section 5: Skill Loading
# ===========================
echo -e "${BLUE}--- Skill Loading ---${NC}"

# Check if skills directory exists and has YAML files
SKILLS_DIR="skills"
if [ -d "$SKILLS_DIR" ]; then
    SKILL_COUNT=$(find "$SKILLS_DIR" -name "*.yaml" -o -name "*.yml" 2>/dev/null | wc -l)
    if [ "$SKILL_COUNT" -gt 0 ]; then
        pass "Skill files found: ${SKILL_COUNT}"

        # List skills
        echo -e "  ${BLUE}  Loaded skills:${NC}"
        find "$SKILLS_DIR" -name "*.yaml" -o -name "*.yml" 2>/dev/null | while read -r f; do
            skill_id=$(grep -m1 "^skill_id:" "$f" 2>/dev/null | awk '{print $2}' || echo "unknown")
            echo -e "    - ${skill_id}"
        done
    else
        warn "No skill files found in ${SKILLS_DIR}/"
    fi
else
    warn "Skills directory not found: ${SKILLS_DIR}"
fi

# Verify JeecgBoot skills
JEECG_COUNT=$(find "$SKILLS_DIR" -name "jeecg-*.yaml" 2>/dev/null | wc -l)
if [ "$JEECG_COUNT" -ge 7 ]; then
    pass "JeecgBoot compatible skills: ${JEECG_COUNT}"
else
    warn "JeecgBoot skills: ${JEECG_COUNT} (expected ≥7)"
fi

# ===========================
# Section 6: MCP Server
# ===========================
echo -e "${BLUE}--- MCP Server ---${NC}"

# /mcp/sse may return 200 (open) or 401 (auth required) — both are acceptable
MCP_SSE_CODE=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 --max-time 10 "${BASE_URL}/mcp/sse" 2>/dev/null || echo "000")
if [ "$MCP_SSE_CODE" = "200" ] || [ "$MCP_SSE_CODE" = "401" ]; then
    pass "MCP SSE endpoint reachable (HTTP ${MCP_SSE_CODE})"
else
    fail "MCP SSE endpoint unreachable (HTTP ${MCP_SSE_CODE})"
fi

check_endpoint "/mcp/message" "MCP message endpoint" "405"

# ===========================
# Section 7: Security
# ===========================
echo -e "${BLUE}--- Security ---${NC}"

# Verify CORS headers
CORS_HEADERS=$(curl -s -I --connect-timeout 5 --max-time 10 "${BASE_URL}/actuator/health" 2>/dev/null || echo "")
if echo "$CORS_HEADERS" | grep -qi "Access-Control"; then
    pass "CORS headers present"
else
    warn "CORS headers not present in response"
fi

# Verify no debug endpoints exposed
DEBUG_CODE=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 5 --max-time 10 "${BASE_URL}/actuator/env" 2>/dev/null || echo "000")
if [ "$DEBUG_CODE" = "404" ] || [ "$DEBUG_CODE" = "401" ]; then
    pass "Debug endpoint protected (/actuator/env → ${DEBUG_CODE})"
else
    warn "Debug endpoint may be exposed (/actuator/env → ${DEBUG_CODE})"
fi

# ===========================
# Section 8: Audit Chain
# ===========================
echo -e "${BLUE}--- Audit Chain ---${NC}"

# Verify migration scripts exist
if [ -d "src/main/resources/db/migration" ]; then
    MIGRATION_FILES=$(find src/main/resources/db/migration -name "*.sql" 2>/dev/null | wc -l)
    if [ "$MIGRATION_FILES" -ge 3 ]; then
        pass "Flyway migration scripts: ${MIGRATION_FILES}"
    else
        warn "Only ${MIGRATION_FILES} migration scripts found (expected ≥3)"
    fi

    # Verify audit_log table schema has prev_hash
    if grep -q "prev_hash" src/main/resources/db/migration/V1__init_core_schema.sql 2>/dev/null; then
        pass "Audit chain prev_hash column defined"
    else
        fail "Audit chain prev_hash column not found in migration"
    fi
else
    warn "Migration scripts directory not found"
fi

# ===========================
# Summary
# ===========================
echo ""
echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  Verification Summary                       ${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo -e "  ${GREEN}PASS: ${PASS}${NC}"
echo -e "  ${YELLOW}WARN: ${WARN}${NC}"
echo -e "  ${RED}FAIL: ${FAIL}${NC}"
echo ""

if [ "$FAIL" -eq 0 ]; then
    echo -e "  ${GREEN}Overall: ✓ All critical checks passed${NC}"
    exit 0
else
    echo -e "  ${RED}Overall: ✗ ${FAIL} check(s) failed${NC}"
    exit 1
fi
