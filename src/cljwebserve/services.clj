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
  (letfn [(modify-parsed-input [parsed-input]
            (if (= (:location parsed-input) "/")
              (assoc parsed-input :location "/index.html")
              parsed-input))]
    (let [header-string (. input readLine)
          [type location protocol] (clojure.string/split header-string #" " )
          header {:type type
                  :location location
                  :protocol protocol}
          parser-regexp #"^([^:]+):[ \t]*(.*)$"]
      (println header-string)
      (let [parsed-input (into header (loop [line (. input readLine)
                                             result {}]
                                        (if (= line "") result
                                            (let [matches (re-find parser-regexp line)
                                                  [key-str val-str] (take 2 (rest matches))]
                                              (recur (. input readLine)
                                                     (into result {(keyword key-str) val-str}))))))]
        (modify-parsed-input parsed-input)))))
        



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
  (let [pwd (str (System/getProperty "user.dir") "/" root)]
    (fn [input output]
      (let [parsed-request (parse-input input)]
        (if (= (get parsed-request :type) "GET")
          (try (let [response (slurp (str pwd (get parsed-request :location)))]
                 (. output print response))
               (catch java.io.FileNotFoundException e (. output println (status 404 "nope"))))))
      (. output close))))

(def handle-files (handle-files-generator ""))

(defn handle-clojure
  [input output]
  (let [parsed-request (parse-input input)]
    (if (= (:type parsed-request) "GET")
      (let [parsed-request (parse-clojure-request (:location parsed-request))]
        (. output print (str (load-string parsed-request))))))
  (. output close))
    
(defn serve-up-this-form-alright?
  [input output]
  (let [parsed-input (parse-input input)
        form-string "<html><body><form action=\"/response\" method=\"POST\"><input name=\"the-field\" type=\"text\" /><input type=\"submit\" value=\"Submit\"></form></body></html>"]
    (cond
      (= (:type parsed-input) "GET") (do
                                       (println "in get")
                                       (. output println form-string)
                                       (. output flush)
                                       (. output close)
                                       )
      (= (:type parsed-input) "POST") (do
                                        (println "in post")
                                        '(let [parsed-input (parse-input input)]
                                           (println "parsed-it")
                                           (. output println (str parsed-input)))
                                        (. output println (. input readLine))
                                        (. output close)))))
(defn read-eval-print
  [input output]
  (let [eof (new Object)
        form (. input readLine)
        result (eval (read form false eof))]
    (println form)
    (println (str result))
    (. output println (str result "\r")))
  (. output close))
    