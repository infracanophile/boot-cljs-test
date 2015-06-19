(ns infracanophile.boot-cljs-test.phantom-runner
  (:require-macros [cljs.test :as t]
                   [infracanophile.boot-cljs-test.phantom-runner :refer [run-tests]])
  (:require
   [cljs.test :as t :refer [report]]
   {{required-ns}}))

(enable-console-print!)

; Junit xml functions
(def ^{:private true}
  escape-xml-map
  (zipmap "'<>\"&" (map #(str \& % \;) '[apos lt gt quot amp])))

(defn- escape-xml [text]
  (apply str (map #(escape-xml-map % %) text)))

(def ^:dynamic *var-context*)
(def ^:dynamic *depth*)

(defn indent
  []
  (dotimes [n (* *depth* 4)] (print " ")))

(defn start-element
  [tag pretty & [attrs]]
  (if pretty (indent))
  (print (str "<" tag))
  (if (seq attrs)
    (doseq [[key value] attrs]
      (print (str " " (name key) "=\"" (escape-xml value) "\""))))
  (print ">")
  (if pretty (println))
  (set! *depth* (inc *depth*)))

(defn element-content
  [content]
  (print (escape-xml content)))

(defn finish-element
  [tag pretty]
  (set! *depth* (dec *depth*))
  (if pretty (indent))
  (print (str "</" tag ">"))
  (if pretty (println)))

(defn test-name
  [vars]
  (apply str (interpose "."
                        (reverse (map #(:name (meta %)) vars)))))

(defn package-class
  [name]
  (let [i (.lastIndexOf name ".")]
    (if (< i 0)
      [nil name]
      [(.substring name 0 i) (.substring name (+ i 1))])))

(defn start-case
  [name classname]
  (start-element 'testcase true {:name name :classname classname}))

(defn finish-case
  []
  (finish-element 'testcase true))

(defn suite-attrs
  [package classname]
  (let [attrs {:name classname}]
    (if package
      (assoc attrs :package package)
      attrs)))

(defn start-suite
  [name]
  (let [[package classname] (package-class name)]
    (start-element 'testsuite true (suite-attrs package classname))))

(defn finish-suite
  []
  (finish-element 'testsuite true))

(defn message-el
  [tag message expected-str actual-str]
  (indent)
  (start-element tag false (if message {:message message} {}))
  (element-content
   (let [detail (apply str (interpose
                            "\n"
                            [(str "expected: " expected-str)
                             (str "  actual: " actual-str)]))]
     (if message (str message "\n" detail) detail)))
  (finish-element tag false)
  (println))

(defn failure-el
  [message expected actual]
  (message-el 'failure message (pr-str expected) (pr-str actual)))

(defn error-el
  [message expected actual]
  (message-el 'error
              message
              (pr-str expected)
              (prn actual)))

; Junit xml reports

(defmethod report [::junit :begin-test-ns] [m]
  (start-suite (name (:ns m))))

(defmethod report [::junit :end-test-ns] [m]
  (finish-suite))

(defmethod report [::junit :begin-test-var] [m]
  (let [var (:var m)]
    (start-case (test-name [var]) (name (:ns (meta var))))))

(defmethod report [::junit :end-test-var] [m]
  (finish-case))

(defmethod report [::junit :pass] [m]
  (t/inc-report-counter! :pass))

(defmethod report [::junit :fail] [m]
  (t/inc-report-counter! :fail)
  (failure-el (:message m)
              (:expected m)
              (:actual m)))

(defmethod report [::junit :error] [m]
  (t/inc-report-counter! :error)
  (error-el (:message m)
            (:expected m)
            (:actual m)))

(defmethod report [::junit :default] [m])

(defmethod report [::junit :summary] [m]
  (println "</testsuites>"))

(defmethod report [::junit :end-run-tests] [m]
  (println "phantom-exit-code:" (if (t/successful? m) 0 1)))

; Default cli reports
(defmethod report [:cljs.test/default :summary] [m]
  (println "\nRan " (:test m) " tests containing")
  (println (+ (:pass m) (:fail m) (:error m)) " assertions.")
  (println (:fail m) " failures, " (:error m) " errors."))

(defmethod report [:cljs.test/default :end-run-tests] [m]
  (println "phantom-exit-code:" (if (t/successful? m) 0 1)))

(defn main []
  (when {{is-junit}}
    (println "<?xml version\"1.0\" encoding=\"UTF-8\"?><testsuites>"))
  (run-tests
      (t/empty-env {{formatter}})
      {{test-predicate}}
      {{tested-ns}}))
