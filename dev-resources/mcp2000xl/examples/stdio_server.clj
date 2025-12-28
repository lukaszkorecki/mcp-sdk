(ns mcp2000xl.examples.stdio-server
  "Example STDIO MCP server with simple tools.

   Demonstrates the stateless STDIO transport for MCP servers."
  (:gen-class)
  (:require [mcp2000xl.server.stdio-stateless :as stdio]))

(def add-tool
  {:name "add"
   :title "Add two numbers"
   :description "Adds two numbers together"
   :input-schema [:map [:a int?] [:b int?]]
   :output-schema [:map [:result int?]]
   :handler (fn [{:keys [a b]}]
              {:result (+ a b)})})

(def hello-resource
  {:url "custom://hello"
   :name "Hello Resource"
   :description "A simple hello resource"
   :mime-type "text/plain"
   :handler (fn [_request]
              ["Hello, World!"])})

(defn -main [& _args]
  (stdio/create
   {:name "example-stdio-server"
    :version "1.0.0"
    :instructions "This is an example MCP server. Use the 'add' tool to add two numbers."
    :tools [add-tool]
    :resources [hello-resource]}))
