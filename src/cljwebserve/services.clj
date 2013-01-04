(ns cljwebserve.services
  (:use [cljwebserve status])
  (:require [clojure string])
  (:use [clojure.contrib.string :only [split]]))

(in-ns 'cljwebserve.services)

(defn- parse-clojure-request
  "Parses a request of as follows:
   _-/+/3/4/5/-_ -> (+ 3 4 5)"
  [clojure-request]
  
  (let [cleaned-request (clojure.string/replace
                         (clojure.string/replace
                          (clojure.string/replace clojure-request #"_-/" "(")
                          #"/-_" ")")
                         #"/" " ")]
    cleaned-request))

(defn- parse-input 
  "Parses a request into a map."
  [input]
  
  (let [header-string (. input readLine)
        [type location protocol] (clojure.string/split header-string #" " )
        header {:type type
                :location location
                :protocol protocol}
        parser-regexp #"^([^:]+):[ \t]*(.*)$"]
    (print header-string)
    (into header (loop [line (. input readLine)
                        result {}]
                   (if (= line "") result
                       (let [matches (re-find parser-regexp line)
                             [key-str val-str] (take 2 (rest matches))]
                         (recur (. input readLine)
                                (into result {(keyword key-str) val-str}))))))))

(defn echo [input output]
  (loop [line (. input readLine)]
    (if (= line "") nil
        (do
          (println line)
          (. output println line)
          (. output flush)
          (recur (. input readLine)))))
  (. output close))

(defn parse-n-print
  [input output]
  (. output println (str (parse-input input)))
  (. output close))

(defn handle-files-generator
  [root]
  (fn [input output]
    (let [parsed-request (parse-input input)
          pwd (str (System/getProperty "user.dir") root)]
      (print parsed-request)
      (if (= (get parsed-request :type) "GET")
        (try (let [response (slurp (str pwd (get parsed-request :location)))]
               (println response)
               (. output print response))
             (catch java.io.FileNotFoundException e (. output println (status 404 "nope"))))))
    (. output close)))

(def handle-files (handle-files-generator ""))

(defn handle-clojure
  [input output]
  (let [parsed-request (parse-input input)]
    (if (= (:type parsed-request) "GET")
      (let [parsed-request (parse-clojure-request (:location parsed-request))]
        (. output print (str (load-string parsed-request))))))
  (. output close))
    
        