#!/usr/bin/env bash
# Run clojure-lsp diagnostics

set -e

echo "Running diagnostics..."
echo

clojure-lsp diagnostics

exit_code=$?

echo
if [ $exit_code -eq 0 ]; then
    echo "✓ No diagnostics issues found"
else
    echo "⚠️  Diagnostics found some issues (see above)"
fi

exit $exit_code
