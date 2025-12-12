(ns fake-server)
(require '[com.latacora.mcp.core :as mcp]
         '[clojure.tools.logging :as log])

(def tool
  (mcp/create-tool-specification
   {:name "add"
    :title "Add two numbers"
    :description "Adds two numbers together"
    :input-schema [:map [:a int?] [:b int?]]
    :output-schema [:map [:result int?]]
    :handler (fn [_exchange {:keys [a b]}]
               {:result (+ a b)})}))

(def resource
  (mcp/create-resource-specification
   {:url "custom://hello"
    :name "Hello Resource"
    :description "A simple hello resource"
    :mime-type "text/plain"
    :handler (fn [_exchange _request]
               ["Hello, World!"])}))

(defn -main []

  (let [mcp-server (mcp/build-mcp-server {:name "hello-world" :version "1.0.0" :tools [tool] :resources [resource]})
        handler (mcp/create-ring-handler mcp-server)
        server (atom nil)]

    (Runtime/.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable
                                                    (fn []
                                                      (try
                                                        (log/warn "Shutting down server...")
                                                        (.close (-> mcp-server :mcp-server))
                                                        (when-let [s @server]
                                                          (.stop s))
                                                        (log/warn "Server shut down.")

                                                        (catch Throwable err
                                                          (log/errorf err "Error during server shutdown"))))))

    (reset! server (mcp/run-jetty handler {:port 2001 :join? true}))))
