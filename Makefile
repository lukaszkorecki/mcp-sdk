.DEFAULT_GOAL := help

help: ## Show this help message
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

fmt: trim ## Format code with clojure-lsp
	clojure-lsp format

trim: ## Remove trailing whitespace from source files
	find src test dev-resources -name "*.clj" -o -name "*.cljc" -o -name "*.cljs" -o -name "*.edn" | xargs sed -i '' 's/[[:space:]]*$$//'

lint: ## Run clojure-lsp diagnostics
	clojure-lsp diagnostics

check: ## Check for reflection warnings
	clojure -M:check

test: ## Run all tests
	clojure -M:dev:test

#
.PHONY: help fmt trim lint check test
