(ns mcp2000xl.tool-builder
  "Build concrete tool specifications from tool definitions.
   
   Converts tool definitions (plain data) into either session-based or stateless
   tool specifications depending on the server type."
  (:require [clojure.tools.logging :as log]
            [jsonista.core :as jsonista]
            [malli.core :as m]
            [malli.error :as me]
            [malli.json-schema :as mjs]
            [malli.transform :as mt])
  (:import (io.modelcontextprotocol.json.jackson JacksonMcpJsonMapper)
           (io.modelcontextprotocol.server McpServerFeatures$SyncToolSpecification
                                           McpStatelessServerFeatures$SyncToolSpecification)
           (io.modelcontextprotocol.spec McpSchema$CallToolRequest
                                         McpSchema$CallToolResult
                                         McpSchema$Tool
                                         McpSchema$ToolAnnotations)
           (java.io PrintWriter StringWriter)
           (java.util.concurrent CompletableFuture)
           (java.util.function BiFunction)))

(set! *warn-on-reflection* true)

(def mcp-mapper
  (JacksonMcpJsonMapper. jsonista/default-object-mapper))

(defn throwable->string [t]
  (let [sw (StringWriter.)]
    (with-open [sw sw
                pw (PrintWriter. sw)]
      (Throwable/.printStackTrace t pw))
    (StringWriter/.toString sw)))

(def malli-transformer
  (mt/transformer
   (mt/json-transformer
    {::mt/keywordize-map-keys true
     ::mt/json-vectors true})
   (mt/default-value-transformer)
   (mt/collection-transformer)))

(defn- build-tool-schema
  "Build MCP Tool schema from tool definition"
  [{:keys [name title description input-schema output-schema
           read-only-hint destructive-hint idempotent-hint
           open-world-hint return-direct meta]}]
  (.build
   (doto (McpSchema$Tool/builder)
     (.name name)
     (.title title)
     (.description description)
     (.inputSchema mcp-mapper (jsonista/write-value-as-string (mjs/transform input-schema)))
     (.outputSchema mcp-mapper (jsonista/write-value-as-string (mjs/transform output-schema)))
     (.annotations (McpSchema$ToolAnnotations. title read-only-hint destructive-hint
                                               idempotent-hint open-world-hint return-direct))
     (.meta meta))))

(defn- create-handler-logic
  "Create the shared handler logic that validates input/output and calls the user's handler"
  [{:keys [name handler input-schema output-schema]}]
  (let [request-coercer (m/decoder input-schema malli-transformer)
        request-explainer (m/explainer input-schema)
        response-coercer (m/decoder output-schema malli-transformer)
        response-explainer (m/explainer output-schema)]
    (fn [request-data]
      (let [clojure-request-data (jsonista/read-value (jsonista/write-value-as-string request-data))
            coerced-request-data (request-coercer clojure-request-data)]
        (if-some [explanation (request-explainer coerced-request-data)]
          ;; Input validation failed
          (do
            (let [ex (ex-info "Invalid request for tool call."
                              {:tool name
                               :request clojure-request-data
                               :explanation (me/humanize explanation)})]
              (log/error ex (ex-message ex)))
            {:error true
             :content (jsonista/write-value-as-string (me/humanize explanation))})
          ;; Input valid, call handler
          (let [response-data (handler coerced-request-data)
                coerced-response-data (response-coercer response-data)]
            (if-some [explanation (response-explainer coerced-response-data)]
              ;; Output validation failed
              (do
                (let [ex (ex-info "Invalid response from tool call."
                                  {:tool name
                                   :request coerced-request-data
                                   :response coerced-response-data
                                   :explanation (me/humanize explanation)})]
                  (log/error ex (ex-message ex)))
                {:error true
                 :content (jsonista/write-value-as-string (me/humanize explanation))})
              ;; All valid
              {:error false
               :content coerced-response-data
               :meta (meta response-data)})))))))

(defn build-session-based-tool
  "Build a session-based tool specification (for STDIO servers)"
  [tool-def]
  (let [tool-schema (build-tool-schema tool-def)
        handler-logic (create-handler-logic tool-def)
        tool-name (:name tool-def)]
    (.build
     (doto (McpServerFeatures$SyncToolSpecification/builder)
       (.tool tool-schema)
       (.callHandler
        (reify BiFunction
          (apply [_this _exchange request]
            (CompletableFuture/supplyAsync
             (fn []
               (try
                 (let [request-data (McpSchema$CallToolRequest/.arguments request)
                       result (handler-logic request-data)]
                   (if (:error result)
                     (.build
                      (doto (McpSchema$CallToolResult/builder)
                        (.isError true)
                        (.addTextContent (:content result))))
                     (.build
                      (doto (McpSchema$CallToolResult/builder)
                        (.structuredContent mcp-mapper (jsonista/write-value-as-string (:content result)))
                        (.meta (or (:meta result) {}))))))
                 (catch Throwable e
                   (let [ex (ex-info "Exception calling tool." {:tool tool-name} e)]
                     (log/error ex (ex-message ex)))
                   (.build
                    (doto (McpSchema$CallToolResult/builder)
                      (.isError true)
                      (.addTextContent (throwable->string e))
                      (.meta (or (meta e) {})))))))))))))))

(defn build-stateless-tool
  "Build a stateless tool specification (for HTTP servers)"
  [tool-def]
  (let [tool-schema (build-tool-schema tool-def)
        handler-logic (create-handler-logic tool-def)
        tool-name (:name tool-def)]
    (.build
     (doto (McpStatelessServerFeatures$SyncToolSpecification/builder)
       (.tool tool-schema)
       (.callHandler
        (reify BiFunction
          (apply [_this _context request]
            (try
              (let [request-data (McpSchema$CallToolRequest/.arguments request)
                    result (handler-logic request-data)]
                (if (:error result)
                  (.build
                   (doto (McpSchema$CallToolResult/builder)
                     (.isError true)
                     (.addTextContent (:content result))))
                  (.build
                   (doto (McpSchema$CallToolResult/builder)
                     (.structuredContent mcp-mapper (jsonista/write-value-as-string (:content result)))
                     (.meta (or (:meta result) {}))))))
              (catch Throwable e
                (let [ex (ex-info "Exception calling tool." {:tool tool-name} e)]
                  (log/error ex (ex-message ex)))
                (.build
                 (doto (McpSchema$CallToolResult/builder)
                   (.isError true)
                   (.addTextContent (throwable->string e))
                   (.meta (or (meta e) {})))))))))))))

(defn build-tools
  "Build tool specifications from tool definitions.
   
   Parameters:
   - tool-defs: Collection of tool definitions
   - server-type: :session-based or :stateless
   
   Returns: Vector of tool specifications"
  [tool-defs server-type]
  (mapv (case server-type
          :session-based build-session-based-tool
          :stateless build-stateless-tool
          (throw (IllegalArgumentException.
                  (str "Unknown server-type: " server-type ". Must be :session-based or :stateless"))))
        tool-defs))
