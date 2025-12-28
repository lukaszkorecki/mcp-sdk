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


inspect-stdio: ## Inspect model context protocol stdio example
	npx @modelcontextprotocol/inspector  clj -M:example-stdio

inspect-http: ## Inspect model context protocol http example
	npx @modelcontextprotocol/inspector  --transport http --server-url http://localhost:8083/mcp clj -M:example-http

#
.PHONY: help fmt trim lint check test
