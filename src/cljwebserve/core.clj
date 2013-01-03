(ns cljwebserve.core
  (:use [cljwebserve services])
  (:import [java.net ServerSocket SocketException])
  (:import [java.io PrintWriter BufferedReader InputStreamReader]))

(in-ns 'cljwebserve.core)

(def coordination-agent (agent 0))
(defn get-reader
  [client]
  (new BufferedReader (new InputStreamReader (. client getInputStream))))

(defn get-writer
  [client]
  (new PrintWriter (. client getOutputStream)))

(defn create-socket
  [port]
  (new ServerSocket port))

(defn run-server
  [port service]
  (let [coordination-agent (agent 0)]
    (letfn [(run-server-agent
              [agent-var port service]
              (let [server-socket (create-socket port)]
                (letfn [(listen-and-respond [server-socket service]
                          (let [request-agent (agent 0)]
                            (let [client (. server-socket accept)
                                  input  (get-reader client)
                                  output (get-writer client)]
                              (send request-agent (fn [dummy input output] (service input output)) input output))))]
                  (while (not (. server-socket isClosed))
                    (listen-and-respond server-socket service)
                    (release-pending-sends)))))]
      (send-off coordination-agent run-server-agent port service))))
