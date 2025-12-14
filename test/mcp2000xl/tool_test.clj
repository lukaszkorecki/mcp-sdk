(ns mcp2000xl.tool-test
  (:require [clojure.test :refer [deftest is]]
            [mcp2000xl.tool :as tool]
            [mcp2000xl.resource :as resource])
  (:import (io.modelcontextprotocol.server McpServerFeatures$SyncToolSpecification
                                           McpServerFeatures$SyncResourceSpecification)))

(deftest can-build-tool-specs
  (is (instance?
       McpServerFeatures$SyncToolSpecification
       (tool/create-tool-specification
        {:name "add"
         :title "Add two numbers"
         :description "Adds two numbers together"
         :input-schema [:map [:a int?] [:b int?]]
         :output-schema [:map [:result int?]]
         :handler (fn [_exchange {:keys [a b]}]
                    {:result (+ a b)})}))))

(deftest can-build-resource-specs
  (is (instance?
       McpServerFeatures$SyncResourceSpecification
       (resource/create-resource-specification
        {:url "custom://test"
         :name "Test Resource"
         :description "A test resource"
         :mime-type "text/plain"
         :handler (fn [_exchange _request]
                    ["Test content"])}))))