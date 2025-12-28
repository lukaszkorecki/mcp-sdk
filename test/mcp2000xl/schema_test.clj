(ns mcp2000xl.schema-test
  (:require [clojure.test :refer [deftest is testing]]
            [mcp2000xl.schema :as schema]))

(deftest valid-tool-definition-test
  (testing "Valid tool passes validation"
    (is (= {:name "test"
            :handler identity
            :input-schema [:map [:x int?]]
            :output-schema [:map [:result int?]]}
           (schema/validate-tool
            {:name "test"
             :handler identity
             :input-schema [:map [:x int?]]
             :output-schema [:map [:result int?]]})))))

(deftest valid-tool-with-optional-fields-test
  (testing "Tool with all optional fields"
    (is (schema/validate-tool
         {:name "test"
          :handler identity
          :input-schema [:map]
          :output-schema [:map]
          :title "Test Tool"
          :description "A test tool"
          :read-only-hint true
          :destructive-hint false
          :idempotent-hint true
          :open-world-hint false
          :return-direct false
          :meta {:foo "bar"}}))))

(deftest missing-required-tool-fields-test
  (testing "Missing :name"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid tool definition"
         (schema/validate-tool
          {:handler identity
           :input-schema [:map]
           :output-schema [:map]}))))

  (testing "Missing :handler"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid tool definition"
         (schema/validate-tool
          {:name "test"
           :input-schema [:map]
           :output-schema [:map]}))))

  (testing "Missing :input-schema"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid tool definition"
         (schema/validate-tool
          {:name "test"
           :handler identity
           :output-schema [:map]}))))

  (testing "Missing :output-schema"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid tool definition"
         (schema/validate-tool
          {:name "test"
           :handler identity
           :input-schema [:map]})))))

(deftest invalid-tool-field-types-test
  (testing ":name must be string"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid tool definition"
         (schema/validate-tool
          {:name 123
           :handler identity
           :input-schema [:map]
           :output-schema [:map]}))))

  (testing ":handler must be function"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid tool definition"
         (schema/validate-tool
          {:name "test"
           :handler "not a function"
           :input-schema [:map]
           :output-schema [:map]}))))

  (testing ":read-only-hint must be boolean"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid tool definition"
         (schema/validate-tool
          {:name "test"
           :handler identity
           :input-schema [:map]
           :output-schema [:map]
           :read-only-hint "yes"})))))

(deftest invalid-malli-schemas-test
  (testing "Invalid :input-schema"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"input-schema must be a valid Malli schema"
         (schema/validate-tool
          {:name "test"
           :handler identity
           :input-schema [:not-a-valid-schema]
           :output-schema [:map]}))))

  (testing "Invalid :output-schema"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"output-schema must be a valid Malli schema"
         (schema/validate-tool
          {:name "test"
           :handler identity
           :input-schema [:map]
           :output-schema [:invalid-schema-type]})))))

(deftest extra-keys-not-allowed-test
  (testing "Closed schema - no extra keys"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid tool definition"
         (schema/validate-tool
          {:name "test"
           :handler identity
           :input-schema [:map]
           :output-schema [:map]
           :unexpected-key "should fail"})))))

(deftest tool-string-constraints-test
  (testing "Tool :name must be non-empty"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid tool definition"
         (schema/validate-tool
          {:name ""
           :handler identity
           :input-schema [:map]
           :output-schema [:map]}))))

  (testing "Tool :name has max length (256)"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid tool definition"
         (schema/validate-tool
          {:name (apply str (repeat 257 "x"))
           :handler identity
           :input-schema [:map]
           :output-schema [:map]}))))

  (testing "Tool :title must be non-empty if provided"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid tool definition"
         (schema/validate-tool
          {:name "test"
           :handler identity
           :input-schema [:map]
           :output-schema [:map]
           :title ""}))))

  (testing "Tool :description has max length (4096)"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid tool definition"
         (schema/validate-tool
          {:name "test"
           :handler identity
           :input-schema [:map]
           :output-schema [:map]
           :description (apply str (repeat 4097 "x"))})))))

(deftest resource-string-constraints-test
  (testing "Resource :url must be non-empty"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid resource definition"
         (schema/validate-resource
          {:url ""
           :name "test"
           :mime-type "text/plain"
           :handler identity}))))

  (testing "Resource :name must be non-empty"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid resource definition"
         (schema/validate-resource
          {:url "file:///test"
           :name ""
           :mime-type "text/plain"
           :handler identity}))))

  (testing "Resource :url has max length (2048)"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid resource definition"
         (schema/validate-resource
          {:url (apply str (repeat 2049 "x"))
           :name "test"
           :mime-type "text/plain"
           :handler identity})))))

(deftest mime-type-validation-test
  (testing "Valid MIME types"
    (is (schema/validate-resource
         {:url "file:///test"
          :name "test"
          :mime-type "text/plain"
          :handler identity}))
    (is (schema/validate-resource
         {:url "file:///test"
          :name "test"
          :mime-type "application/json"
          :handler identity}))
    (is (schema/validate-resource
         {:url "file:///test"
          :name "test"
          :mime-type "image/svg+xml"
          :handler identity}))
    (is (schema/validate-resource
         {:url "file:///test"
          :name "test"
          :mime-type "text/html; charset=utf-8"
          :handler identity})))

  (testing "Invalid MIME types"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid resource definition"
         (schema/validate-resource
          {:url "file:///test"
           :name "test"
           :mime-type "not a mime type"
           :handler identity})))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid resource definition"
         (schema/validate-resource
          {:url "file:///test"
           :name "test"
           :mime-type "/json" ; Missing type
           :handler identity})))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid resource definition"
         (schema/validate-resource
          {:url "file:///test"
           :name "test"
           :mime-type "text/" ; Missing subtype
           :handler identity})))))

(deftest valid-resource-definition-test
  (testing "Valid resource passes validation"
    (is (= {:url "file:///test.txt"
            :name "test"
            :mime-type "text/plain"
            :handler identity}
           (schema/validate-resource
            {:url "file:///test.txt"
             :name "test"
             :mime-type "text/plain"
             :handler identity})))))

(deftest valid-resource-with-description-test
  (testing "Resource with description"
    (is (schema/validate-resource
         {:url "file:///test.txt"
          :name "test"
          :mime-type "text/plain"
          :handler identity
          :description "A test resource"}))))

(deftest missing-required-resource-fields-test
  (testing "Missing :url"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid resource definition"
         (schema/validate-resource
          {:name "test"
           :mime-type "text/plain"
           :handler identity}))))

  (testing "Missing :name"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid resource definition"
         (schema/validate-resource
          {:url "file:///test.txt"
           :mime-type "text/plain"
           :handler identity}))))

  (testing "Missing :mime-type"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid resource definition"
         (schema/validate-resource
          {:url "file:///test.txt"
           :name "test"
           :handler identity}))))

  (testing "Missing :handler"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid resource definition"
         (schema/validate-resource
          {:url "file:///test.txt"
           :name "test"
           :mime-type "text/plain"})))))

(deftest invalid-resource-field-types-test
  (testing ":handler must be function"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid resource definition"
         (schema/validate-resource
          {:url "file:///test.txt"
           :name "test"
           :mime-type "text/plain"
           :handler "not a function"})))))

(deftest validate-collections-test
  (testing "Validate multiple tools"
    (is (schema/validate-tools
         [{:name "tool1"
           :handler identity
           :input-schema [:map]
           :output-schema [:map]}
          {:name "tool2"
           :handler identity
           :input-schema [:map]
           :output-schema [:map]}])))

  (testing "Validate multiple resources"
    (is (schema/validate-resources
         [{:url "file:///1.txt"
           :name "resource1"
           :mime-type "text/plain"
           :handler identity}
          {:url "file:///2.txt"
           :name "resource2"
           :mime-type "text/plain"
           :handler identity}])))

  (testing "Collection validation fails on first invalid item"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Invalid tool definition"
         (schema/validate-tools
          [{:name "good"
            :handler identity
            :input-schema [:map]
            :output-schema [:map]}
           {:name "bad"
            ;; Missing handler
            :input-schema [:map]
            :output-schema [:map]}])))))
