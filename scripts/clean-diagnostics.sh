#!/usr/bin/env bash
# Run diagnostics but filter out known acceptable warnings

set -e

echo "Running clojure-lsp diagnostics (filtering known acceptable warnings)..."
echo

# Run diagnostics and capture output
output=$(clojure-lsp diagnostics 2>&1)
exit_code=$?

# Filter out acceptable warnings
filtered=$(echo "$output" | grep -v "unused-public-var.*server.stdio/create" || true)

# Display filtered output
echo "$filtered"

# Parse filtered output for different severity levels
errors=$(echo "$filtered" | grep -c "severity :error" || true)
warnings=$(echo "$filtered" | grep -c "severity :warning" || true)
info=$(echo "$filtered" | grep -c "severity :info" || true)
unused=$(echo "$filtered" | grep -c "unused" || true)

echo
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Summary (after filtering known acceptable warnings):"
echo "  Errors:   $errors"
echo "  Warnings: $warnings"
echo "  Info:     $info"
echo "  Unused:   $unused"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo

if [ "$errors" -gt 0 ]; then
    echo "❌ Found $errors error(s)"
    exit 1
elif [ "$warnings" -gt 0 ] || [ "$info" -gt 0 ] || [ "$unused" -gt 0 ]; then
    echo "⚠️  Found issues - review warnings, info, and unused code above"
    exit 1
else
    echo "✓ No issues found"
    exit 0
fi
