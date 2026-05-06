#!/bin/bash
# sync-api.sh — Sync OpenAPI contract between backend and frontend
#
# Usage: ./scripts/sync-api.sh
#
# Steps:
# 1. Fetch OpenAPI YAML from running backend
# 2. Generate TypeScript API client
# 3. Check for uncommitted API contract changes

set -euo pipefail

API_URL="${API_BASE_URL:-http://localhost:8080}"
FRONTEND_DIR="$(dirname "$0")/../frontend"

echo "=== Aurora API Sync ==="
echo "Backend: $API_URL"

# Step 1: Fetch OpenAPI spec
echo ""
echo "[1/3] Fetching OpenAPI spec..."
curl -sf "$API_URL/v3/api-docs.yaml" -o "$FRONTEND_DIR/api-docs.yaml" || {
    echo "ERROR: Failed to fetch OpenAPI spec from $API_URL"
    echo "Make sure the backend is running."
    exit 1
}
echo "  Saved to $FRONTEND_DIR/api-docs.yaml"

# Step 2: Generate TypeScript client
echo ""
echo "[2/3] Generating TypeScript API client..."
cd "$FRONTEND_DIR"
if command -v pnpm &> /dev/null; then
    pnpm generate:api || {
        echo "WARNING: API client generation failed"
    }
else
    echo "WARNING: pnpm not found, skipping generation"
fi
cd - > /dev/null

# Step 3: Check for uncommitted changes
echo ""
echo "[3/3] Checking for uncommitted API changes..."
if command -v git &> /dev/null; then
    CHANGED=$(git diff --name-only -- "$FRONTEND_DIR/src/api/generated/" 2>/dev/null || true)
    if [ -n "$CHANGED" ]; then
        echo "WARNING: API contract has changed:"
        echo "$CHANGED"
        echo ""
        echo "Please commit these changes."
    else
        echo "  No uncommitted API changes."
    fi
fi

echo ""
echo "=== Sync Complete ==="
