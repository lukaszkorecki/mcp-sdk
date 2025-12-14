#!/usr/bin/env bash
# Check for reflection warnings in all namespaces

set -e

echo "Checking for reflection warnings..."
echo

output=$(clj -M:dev -e "(require 'mcp2000xl.tool 'mcp2000xl.resource 'mcp2000xl.server) (println \"✓ All namespaces loaded successfully\")" 2>&1)

# Check if there are any reflection warnings
if echo "$output" | grep -i "reflection warning" > /dev/null; then
    echo "❌ Reflection warnings found:"
    echo
    echo "$output" | grep -i "reflection warning"
    exit 1
else
    echo "✓ No reflection warnings found"
    exit 0
fi
