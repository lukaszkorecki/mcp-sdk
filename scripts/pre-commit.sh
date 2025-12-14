#!/usr/bin/env bash
# Pre-commit check: runs all quality checks

set -e

echo "================================"
echo "Running pre-commit checks..."
echo "================================"
echo

# Format
echo "1. Formatting code..."
./scripts/format.sh
echo

# Lint
echo "2. Running linter..."
./scripts/lint.sh || true  # Don't fail on warnings
echo

# Reflection check
echo "3. Checking for reflection warnings..."
./scripts/check-reflection.sh
echo

# Tests
echo "4. Running tests..."
./scripts/test.sh
echo

echo "================================"
echo "✓ All checks passed!"
echo "================================"
