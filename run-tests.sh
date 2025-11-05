#!/bin/bash

echo "========================================"
echo "Running Tests - MVT Events"
echo "========================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Navigate to project directory
cd "$(dirname "$0")"

echo "📋 Step 1: Checking Docker containers..."
echo ""
DOCKER_STATUS=$(docker ps --filter "name=mvt-events-db" --format "{{.Status}}" 2>/dev/null)
if [ -n "$DOCKER_STATUS" ]; then
    echo "✅ Database container is running: $DOCKER_STATUS"
else
    echo "⚠️  Database container not running (OK for tests - using H2)"
fi

echo ""
echo "📋 Step 2: Cleaning previous build..."
echo ""
./gradlew clean --quiet

echo ""
echo "📋 Step 3: Running tests..."
echo ""
./gradlew test --info

TEST_EXIT_CODE=$?

echo ""
echo "========================================"
if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✅ Tests PASSED!${NC}"
    echo "========================================"
    echo ""
    echo "📊 Test Report: build/reports/tests/test/index.html"
    echo ""
    
    # Try to open test report
    if command -v open &> /dev/null; then
        echo "Opening test report in browser..."
        open build/reports/tests/test/index.html
    fi
else
    echo -e "${RED}❌ Tests FAILED!${NC}"
    echo "========================================"
    echo ""
    echo "📊 Test Report: build/reports/tests/test/index.html"
    echo "📋 Full output in: build/test-results/"
    echo ""
    echo "Common issues:"
    echo "  1. Database connection (should use H2 for tests)"
    echo "  2. Missing dependencies"
    echo "  3. Entity configuration issues"
    echo ""
fi

exit $TEST_EXIT_CODE
