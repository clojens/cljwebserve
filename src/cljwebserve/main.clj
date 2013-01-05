(ns cljwebserve.main
  (:gen-class)
  (:require [cljwebserve core services]))
(in-ns 'cljwebserve.main)

(defn -main
  [port root]
  (cljwebserve.core/run-server port (cljwebserve.services/handle-files-generator root)))
