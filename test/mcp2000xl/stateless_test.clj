(ns mcp2000xl.stateless-test
  (:require [clojure.test :refer [deftest is testing]]
            [mcp2000xl.stateless :as stateless]
            [cheshire.core :as json]))

(def add-tool
  {:name "add"
   :title "Add Two Numbers"
   :description "Adds two numbers together"
   :input-schema [:map
                  [:a int?]
                  [:b int?]]
   :output-schema [:map
                   [:result int?]]
   :handler (fn [{:keys [a b]}]
              {:result (+ a b)})})

(def readme-resource
  {:url "custom://readme"
   :name "Project README"
   :description "The project's README file"
   :mime-type "text/markdown"
   :handler (fn [_request]
              ["# Test README\n\nThis is a test."])})

(def mcp-handler (stateless/create-handler
                  {:name "test-server"
                   :version "1.0.0"
                   :tools [add-tool]
                   :resources [readme-resource]}))

(deftest test-create-handler
  (testing "Can create a stateless handler"
    (is (some? mcp-handler) "Handler should be created")
    (is (instance? io.modelcontextprotocol.server.McpStatelessServerHandler mcp-handler)
        "Handler should implement McpStatelessServerHandler")))

(deftest test-invoke-tool-call
  (testing "Can invoke a tool and get a response"
    (let [request {:jsonrpc "2.0"
                   :id 1
                   :method "tools/call"
                   :params {:name "add"
                            :arguments {:a 5 :b 3}}}

          response (stateless/invoke mcp-handler request)]

      (is (= :x response)))))

(deftest test-invoke-with-keywords
  (testing "Can invoke with keyword keys (they get stringified)"
    (let [handler (stateless/create-handler
                   {:name "test-server"
                    :version "1.0.0"
                    :tools [add-tool]})

          ;; Request with keyword keys
          request {:jsonrpc "2.0"
                   :id 2
                   :method "tools/call"
                   :params {:name "add"
                            :arguments {:a 10 :b 20}}}

          response (stateless/invoke handler request)]

      (is (= "2.0" (:jsonrpc response)))
      (is (= 2 (:id response)))
      (is (some? (:result response)))
      (is (nil? (:error response))))))

(deftest test-invoke-list-tools
  (testing "Can list available tools"
    (let [handler (stateless/create-handler
                   {:name "test-server"
                    :version "1.0.0"
                    :tools [add-tool]})

          request {:jsonrpc "2.0"
                   :id 3
                   :method "tools/list"
                   :params {}}

          response (stateless/invoke handler request)]

      (is (= "2.0" (:jsonrpc response)))
      (is (= 3 (:id response)))
      (is (some? (:result response)) "Should have result")
      (is (nil? (:error response)) "Should not have error"))))

(deftest test-invoke-with-various-types
  (testing "Can handle various JSON types in params"
    (let [handler (stateless/create-handler
                   {:name "test-server"
                    :version "1.0.0"
                    :tools [add-tool]})

          ;; Test with nested structures, arrays, etc
          request {:jsonrpc "2.0"
                   :id "string-id"
                   :method "tools/call"
                   :params {:name "add"
                            :arguments {:a 1 :b 2}
                            :metadata {:tags ["test" "demo"]
                                       :enabled true
                                       :count 42}}}

          response (stateless/invoke handler request)]

      (is (= "2.0" (:jsonrpc response)))
      (is (= "string-id" (:id response)) "Should handle string IDs")
      (is (some? (:result response))))))

(deftest test-invoke-error-handling
  (testing "Returns error on invalid method"
    (let [handler (stateless/create-handler
                   {:name "test-server"
                    :version "1.0.0"
                    :tools [add-tool]})

          request {:jsonrpc "2.0"
                   :id 999
                   :method "nonexistent/method"
                   :params {}}

          response (stateless/invoke handler request)]

      (is (= "2.0" (:jsonrpc response)))
      (is (= 999 (:id response)))
      (is (some? (:error response)) "Should have error")
      (is (nil? (:result response)) "Should not have result"))))

(deftest test-create-handler-with-resources
  (testing "Can create handler with resources"
    (let [handler (stateless/create-handler
                   {:name "test-server"
                    :version "1.0.0"
                    :tools [add-tool]
                    :resources [readme-resource]})]
      (is (some? handler) "Handler should be created with resources"))))

(deftest test-invoke-list-resources
  (testing "Can list available resources"
    (let [handler (stateless/create-handler
                   {:name "test-server"
                    :version "1.0.0"
                    :resources [readme-resource]})

          request {:jsonrpc "2.0"
                   :id 10
                   :method "resources/list"
                   :params {}}

          response (stateless/invoke handler request)]

      (is (= "2.0" (:jsonrpc response)))
      (is (= 10 (:id response)))
      (is (some? (:result response)) "Should have result")
      (is (nil? (:error response)) "Should not have error"))))

(deftest test-invoke-read-resource
  (testing "Can read a resource"
    (let [handler (stateless/create-handler
                   {:name "test-server"
                    :version "1.0.0"
                    :resources [readme-resource]})

          request {:jsonrpc "2.0"
                   :id 11
                   :method "resources/read"
                   :params {:uri "custom://readme"}}

          response (stateless/invoke handler request)]

      (is (= "2.0" (:jsonrpc response)))
      (is (= 11 (:id response)))
      (is (some? (:result response)) "Should have result")
      (is (nil? (:error response)) "Should not have error"))))

(deftest test-invoke-returns-clojure-data
  (testing "invoke returns pure Clojure data structures, not Java objects"
    (let [handler (stateless/create-handler
                   {:name "test-server"
                    :version "1.0.0"
                    :tools [add-tool]
                    :resources [readme-resource]})

          initialize-request {:jsonrpc "2.0"
                              :id 1
                              :method "initialize"
                              :params {:protocolVersion "2024-11-05"
                                       :capabilities {}
                                       :clientInfo {:name "test-client"
                                                    :version "1.0.0"}}}

          init-response (stateless/invoke handler initialize-request)]

      (testing "Response is a Clojure map"
        (is (map? init-response) "Response should be a map")
        (is (instance? clojure.lang.IPersistentMap init-response)
            "Response should be a persistent map"))

      (testing "Result field is Clojure data"
        (let [result (:result init-response)]
          (is (some? result) "Result should exist")
          (is (map? result) "Result should be a map")
          (is (instance? clojure.lang.IPersistentMap result)
              "Result should NOT be a Java MCP class")
          (is (contains? result :serverInfo) "Result should have serverInfo as keyword")
          (is (= "test-server" (get-in result [:serverInfo :name]))
              "Should be able to access nested data with keywords")))

      (testing "Error field is Clojure data when present"
        (let [error-request {:jsonrpc "2.0"
                             :id 2
                             :method "invalid/method"
                             :params {}}
              error-response (stateless/invoke handler error-request)
              error (:error error-response)]

          (is (some? error) "Error should exist for invalid method")
          (is (map? error) "Error should be a map")
          (is (instance? clojure.lang.IPersistentMap error)
              "Error should NOT be a Java MCP class")
          (is (contains? error :code) "Error should have code")
          (is (contains? error :message) "Error should have message"))))))

(deftest test-tools-list-returns-clojure-data
  (testing "tools/list returns Clojure data structures"
    (let [handler (stateless/create-handler
                   {:name "test-server"
                    :version "1.0.0"
                    :tools [add-tool]})

          request {:jsonrpc "2.0"
                   :id 1
                   :method "tools/list"
                   :params {}}

          response (stateless/invoke handler request)
          result (:result response)]

      (is (map? result) "Result should be a map")
      (is (instance? clojure.lang.IPersistentMap result)
          "Result should NOT be a Java MCP class")
      (is (contains? result :tools) "Should have :tools key")
      (is (vector? (:tools result)) "Tools should be a vector")
      (is (every? map? (:tools result)) "Each tool should be a map"))))

(deftest test-tool-call-returns-clojure-data
  (testing "tools/call returns Clojure data structures"
    (let [handler (stateless/create-handler
                   {:name "test-server"
                    :version "1.0.0"
                    :tools [add-tool]})

          request {:jsonrpc "2.0"
                   :id 1
                   :method "tools/call"
                   :params {:name "add"
                            :arguments {:a 5 :b 3}}}

          response (stateless/invoke handler request)
          result (:result response)]

      (is (map? result) "Result should be a map")
      (is (instance? clojure.lang.IPersistentMap result)
          "Result should NOT be a Java MCP class")
      (is (contains? result :content) "Should have :content key")
      (is (or (vector? (:content result)) (map? (:content result)))
          "Content should be a Clojure collection"))))

(deftest test-resources-list-returns-clojure-data
  (testing "resources/list returns Clojure data structures"
    (let [handler (stateless/create-handler
                   {:name "test-server"
                    :version "1.0.0"
                    :resources [readme-resource]})

          request {:jsonrpc "2.0"
                   :id 1
                   :method "resources/list"
                   :params {}}

          response (stateless/invoke handler request)
          result (:result response)]

      (is (map? result) "Result should be a map")
      (is (instance? clojure.lang.IPersistentMap result)
          "Result should NOT be a Java MCP class")
      (is (contains? result :resources) "Should have :resources key")
      (is (vector? (:resources result)) "Resources should be a vector")
      (is (every? map? (:resources result)) "Each resource should be a map"))))

(deftest test-resource-read-returns-clojure-data
  (testing "resources/read returns Clojure data structures"
    (let [handler (stateless/create-handler
                   {:name "test-server"
                    :version "1.0.0"
                    :resources [readme-resource]})

          request {:jsonrpc "2.0"
                   :id 1
                   :method "resources/read"
                   :params {:uri "custom://readme"}}

          response (stateless/invoke handler request)
          result (:result response)]

      (is (map? result) "Result should be a map")
      (is (instance? clojure.lang.IPersistentMap result)
          "Result should NOT be a Java MCP class")
      (is (contains? result :contents) "Should have :contents key")
      (is (vector? (:contents result)) "Contents should be a vector"))))

(deftest test-response-can-be-json-serialized
  (testing "Response from invoke can be serialized to JSON with any JSON library"
    (let [handler (stateless/create-handler
                   {:name "test-server"
                    :version "1.0.0"
                    :tools [add-tool]})

          request {:jsonrpc "2.0"
                   :id 1
                   :method "tools/list"
                   :params {}}

          response (stateless/invoke handler request)]

      (testing "Can serialize with Cheshire"
        (is (string? (json/generate-string response))
            "Should be able to serialize response with Cheshire"))

      (testing "Serialized response is valid JSON"
        (let [json-str (json/generate-string response)
              parsed (json/parse-string json-str true)]
          (is (map? parsed) "Parsed JSON should be a map")
          (is (= "2.0" (:jsonrpc parsed))
              "Should preserve jsonrpc field"))))))
