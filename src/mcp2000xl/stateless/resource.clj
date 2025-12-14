(ns mcp2000xl.stateless.resource
  "Resource specifications for stateless MCP servers"
  (:require [clojure.tools.logging :as log])
  (:import (io.modelcontextprotocol.server McpStatelessServerFeatures$SyncResourceSpecification)
           (io.modelcontextprotocol.spec McpSchema$Resource McpSchema$TextResourceContents McpSchema$ReadResourceResult)
           (java.util.function BiFunction)))

(set! *warn-on-reflection* true)

(defn create-resource-specification
  "Create stateless MCP resource specification.
   
   Options:
   - :url - The URL/URI of the resource (e.g., \"custom://my-resource\")
   - :name - The name of the resource
   - :description - A description of what the resource provides
   - :mime-type - The MIME type (e.g., \"text/plain\", \"text/markdown\")
   - :handler - Function (fn [context request] ...) returns vector of strings
   
   Example:
   (create-resource-specification
     {:url \"custom://readme\"
      :name \"Project README\"
      :description \"The project's README file\"
      :mime-type \"text/markdown\"
      :handler (fn [_context _request]
                 [(slurp \"README.md\")])})"
  [{:keys [url name description mime-type handler]}]
  (let [resource (McpSchema$Resource/builder)
        _ (doto resource
            (.uri url)
            (.name name)
            (.description description)
            (.mimeType mime-type))
        resource-obj (.build resource)]

    (McpStatelessServerFeatures$SyncResourceSpecification.
     resource-obj
     (reify BiFunction
       (apply [_this context request]
         (try
           (let [result-strings (handler context request)
                 resource-contents (mapv #(McpSchema$TextResourceContents. url mime-type %)
                                         result-strings)]
             (McpSchema$ReadResourceResult. resource-contents))
           (catch Throwable e
             (let [ex (ex-info "Exception calling resource handler."
                               {:url url :name name :request request} e)]
               (log/error ex (ex-message ex)))
             (let [error-content (McpSchema$TextResourceContents.
                                  url
                                  "text/plain"
                                  (str "Error retrieving resource: " (ex-message e)))]
               (McpSchema$ReadResourceResult. [error-content])))))))))
