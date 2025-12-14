#!/usr/bin/env bash
# Run all tests

set -e

echo "Running all tests..."
echo

clj -M:test

echo
echo "✓ All tests passed"
