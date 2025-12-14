#!/usr/bin/env bash
# Format all Clojure files using clojure-lsp

set -e

echo "Formatting code with clojure-lsp..."
echo

clojure-lsp format

echo
echo "✓ Formatting complete"
