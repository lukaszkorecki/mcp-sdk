#!/usr/bin/env bash
# Check for reflection warnings in all namespaces

set -e

echo "Checking for reflection warnings..."
echo

# Find all Clojure source files and extract namespaces
namespaces=$(find src -name "*.clj" -type f | while read -r file; do
    grep -E "^\(ns " "$file" | sed 's/(ns //' | sed 's/ .*//' | sed 's/)$//'
done | sort)

if [ -z "$namespaces" ]; then
    echo "❌ No namespaces found in src/"
    exit 1
fi

echo "Found $(echo "$namespaces" | wc -l | tr -d ' ') namespaces to check:"
echo "$namespaces" | sed 's/^/  - /'
echo

# Build require form for all namespaces
require_form=$(echo "$namespaces" | awk '{print "'\''mcp2000xl." $1}' | tr '\n' ' ')

# Load all namespaces with reflection warnings enabled
output=$(clj -M:dev -e "(set! *warn-on-reflection* true) $(echo "$namespaces" | while read -r ns; do echo "(require '$ns)"; done) (println \"\\n✓ All namespaces loaded successfully\")" 2>&1)

# Check if there are any reflection warnings
if echo "$output" | grep -i "reflection warning" > /dev/null; then
    echo "❌ Reflection warnings found:"
    echo
    echo "$output" | grep -i "reflection warning"
    exit 1
else
    echo "$output" | tail -2
    exit 0
fi
