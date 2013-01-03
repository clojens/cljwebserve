(ns cljwebserve.status
  (:use [clojure.contrib.pprint :only [cl-format]]))
(in-ns 'cljwebserve.status)

(defn status
  ([number] (cl-format nil "HTTP/1.1 ~a" number))
  ([number reason] (cl-format nil "HTTP/1.1 ~a ~a" number reason))
  )