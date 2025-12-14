(ns mcp2000xl.tool-def
  "Unified tool definitions for MCP servers.
   
   Tools are defined as plain data maps, then built into the appropriate
   specification type (session-based or stateless) when creating a server."
  (:require [malli.core :as m]
            [malli.json-schema :as mjs]))

(defn tool-definition
  "Define a tool as plain data. This definition can be used to create either
   session-based or stateless tool specifications.
   
   Options:
   - :name (required) - Tool name
   - :title - Tool title
   - :description - Tool description
   - :input-schema - Malli schema for input validation
   - :output-schema - Malli schema for output validation
   - :handler (required) - Function (fn [args] result) that processes the tool call
   - :read-only-hint - Hint that tool is read-only (default: false)
   - :destructive-hint - Hint that tool is destructive (default: false)
   - :idempotent-hint - Hint that tool is idempotent (default: false)
   - :open-world-hint - Hint for open world (default: false)
   - :return-direct - Return directly to user (default: false)
   - :meta - Metadata map (default: {})
   
   Example:
   (tool-definition
     {:name \"add\"
      :description \"Adds two numbers\"
      :input-schema [:map [:a int?] [:b int?]]
      :output-schema [:map [:result int?]]
      :handler (fn [{:keys [a b]}] {:result (+ a b)})})"
  [{:keys [name
           title
           description
           input-schema
           output-schema
           handler
           read-only-hint
           destructive-hint
           idempotent-hint
           open-world-hint
           return-direct
           meta]
    :or {read-only-hint false
         destructive-hint false
         idempotent-hint false
         open-world-hint false
         return-direct false
         meta {}}
    :as tool-def}]
  (when-not name
    (throw (IllegalArgumentException. "Tool :name is required")))
  (when-not handler
    (throw (IllegalArgumentException. "Tool :handler is required")))
  (when-not input-schema
    (throw (IllegalArgumentException. "Tool :input-schema is required")))
  (when-not output-schema
    (throw (IllegalArgumentException. "Tool :output-schema is required")))
  
  ;; Validate schemas are valid Malli schemas
  (when-not (m/schema? input-schema)
    (throw (IllegalArgumentException. "Tool :input-schema must be a valid Malli schema")))
  (when-not (m/schema? output-schema)
    (throw (IllegalArgumentException. "Tool :output-schema must be a valid Malli schema")))
  
  ;; Return validated tool definition with defaults
  (assoc tool-def
         :read-only-hint read-only-hint
         :destructive-hint destructive-hint
         :idempotent-hint idempotent-hint
         :open-world-hint open-world-hint
         :return-direct return-direct
         :meta meta))

(defn validate-tool-definitions
  "Validate a collection of tool definitions. Returns nil if valid, throws otherwise."
  [tool-defs]
  (doseq [tool-def tool-defs]
    (tool-definition tool-def))
  nil)
