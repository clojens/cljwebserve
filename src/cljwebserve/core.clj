(ns cljwebserve.core
  (:use [cljwebserve services])
  (:require [cljwebserve.util :as util])
  (:require [clojure.contrib.pprint :as pp])
  (:import [java.net ServerSocket SocketException])
  (:import [java.io PrintWriter BufferedReader InputStreamReader]))

(in-ns 'cljwebserve.core)

(def servers (agent {}))

(defn get-reader
  "Wrapper function which creates a reader from a given client."
  [client]
  (new BufferedReader (new InputStreamReader (. client getInputStream))))

(defn get-writer
  "Wrapper function which creates a writer to a given client"
  [client]
  (new PrintWriter (. client getOutputStream)))

(defn create-socket
  "Wrapper function which creates a socket on a given port"
  [port]
  (new ServerSocket port))

(defn run-server
  "Starts a server which provides service on port."
  [port service]
  (if (contains? @servers (apply (comp keyword str) [port]))
    (throw
     (java.net.SocketException. (pp/cl-format nil "Already serving on port ~a." port))))
  (let [coordination-agent (agent 0)]
    (letfn [(structure-server-data
              [server-agent port service server-socket]
              {(keyword (str port)) {:agent server-agent
                                     :service service
                                     :port port
                                     :socket server-socket}})
            (run-server-agent
              [agent-var port service]
              (let [server-socket (create-socket port)]
                (letfn [(listen-and-respond [server-agent server-socket service]
                          (let [client (. server-socket accept)
                                input  (get-reader client)
                                output (get-writer client)]
                            (send server-agent (fn [dummy input output] (service input output)) input output)))]
                  (let [server-agent (agent 0)]
                    
                    (util/with-release
                      (send
                       servers
                       (fn [map-agent
                            server-agent
                            port
                            service
                            server-socket]
                         (into map-agent (structure-server-data server-agent
                                                                port
                                                                service
                                                                server-socket)))
                       server-agent
                       port
                       service
                       server-socket))
                    (while (not (. server-socket isClosed))
                      (util/with-release
                        (listen-and-respond server-agent server-socket service)))))))]
      (send-off coordination-agent run-server-agent port service))))

(defn stop-server
  "Stops the server that is running on the specified port."
  [port]
  (util/agentify [servers _*_ (dissoc _*_ (keyword (str port)))]
    (let [keyport (keyword (str port))
          server-structure (keyport @servers)]
      (. (:socket server-structure) close))))
      

    
  