(ns infracanophile.boot-cljs-test
  (:import [boot App]
           [java.io File]
           [java.lang StackTraceElement])
  (:require [clojure.java.io :as io]
            [clojure.tools.namespace.parse :as namespace-parse]
            [clojure.tools.namespace.file :as namespace-file]
            [clojure.string :as string]
            [me.raynes.conch :as conch]
            [boot.core :as core :refer [deftask]]
            [boot.util :as util :refer [sh]]
            [boot.file :as file]
            [pandeiro.boot-http :refer [serve]]
            [clostache.parser :refer [render render-resource]]))

(defn required-ns
  "Receives a list of namespaces. Generates a string that can be fed
  to the placeholder inside `(:require ...)` form"
  [namespaces]
  (->> (map #(str "[" % "]") namespaces)
       (clojure.string/join "\n   ")))

(defn tested-ns
  "Receives a list of namespaces. Generates a string that can be fed
  to the placeholder inside `(run-test ...)` form"
  [namespaces]
  (->> (map #(str "'" %) namespaces)
       (clojure.string/join " ")))

(defn mk-parents [file]
  (-> file .getParent io/file .mkdirs))

;; Fix for finding cljc namespaces.
(defn read-ns-decl [rdr]
  (try
    (loop []
      (let [form (doto (read {:read-cond :allow} rdr) str)]
        (if (namespace-parse/ns-decl? form)
          form
          (recur))))
    (catch Exception e nil)))

(defn read-file-ns-decl [file]
  (with-open [rdr (java.io.PushbackReader. (io/reader file))]
    (read-ns-decl rdr)))

(defn clojurescript-file?
  [^File file]
  (and (.isFile file)
       (or
         (.endsWith (.getName file) ".cljs")
         (.endsWith (.getName file) ".cljc"))))

(defn find-clojurescript-sources-in-dir
  [^File dir]
  (sort-by #(.getAbsolutePath ^File %)
           (filter clojurescript-file? (file-seq dir))))

(defn find-ns-decls-in-dir [^java.io.File dir]
  (keep read-file-ns-decl (find-clojurescript-sources-in-dir dir)))

(defn find-namespaces-in-dir [^java.io.File dir]
  (map second (find-ns-decls-in-dir dir)))

;; Get all namespaces.
(defn get-all-ns [& dirs]
  (-> (mapcat #(find-namespaces-in-dir (io/file %)) dirs)))

(defn filter-namespaces
  [regex namespaces]
  (filter #(re-find regex (str %)) namespaces))

(deftask cljs-test-runner
  "Automatically produces:

  - a Clojurescript source that will run all tests in given `namespaces` list.
  - an EDN file to instruct `boot-cljs` to build the file targeting the browser.

  Should be called before `boot-cljs` task."
  [n namespaces NS #{sym} "The set of namespace symbols to run test in."
   l limit-regex REGEX regex "A regex for limiting namespaces to be tested"
   t test-filters EXPR #{edn} "The set of expressions to use to filter tests"
   f formatter FORMATTER kw "Tag defining formatter to use. Accepts `junit`. Defaults to standard clojure.test output"]
  (when (and namespaces limit-regex)
    (throw (Exception. (str "cljs-test-runner: Providing explicit namespaces"
                            " and a limit-regex is not supported"))))
  (let [templates {:sources
                   ["infracanophile/boot_cljs_test/phantom_runner.cljs"
                    "cljs_test_phantom_runner.cljs.edn"]
                   :assets
                   ["phantom_wrapper.js"
                    "phantom_wrapper.js.html"]}
        test-dir (core/tmp-dir!)
        asset-dir (core/tmp-dir!)]
    (core/with-pre-wrap fileset
      (println "Starting test...")
      (file/empty-dir! test-dir)
      (let [namespaces (if namespaces
                         (seq namespaces)
                         (let [filter-ns-fn (if limit-regex
                                              #(filter-namespaces limit-regex %)
                                              #(filter-namespaces #"-test$" %))]
                           (->> fileset
                                core/input-dirs
                                (map (memfn getPath))
                                (apply get-all-ns)
                                filter-ns-fn)))
            test-predicate (if test-filters
                            `(~'fn [~'%] (~'and ~@test-filters))
                            `(~'fn [~'%] true))
            data {:required-ns (required-ns namespaces)
                  :tested-ns (tested-ns namespaces)
                  :test-predicate test-predicate
                  :is-junit (= :junit formatter)
                  :formatter (if (= :junit formatter)
                               :infracanophile.boot-cljs-test.phantom-runner/junit
                               :cljs.test/default)}]
        (doseq [template (:sources templates)
                :let [output (io/file test-dir template)
                      content (render-resource template data)]]
          (mk-parents output)
          (spit output content))
        (file/empty-dir! asset-dir)
        (doseq [template (:assets templates)
                :let [output (io/file asset-dir template)
                      content (render-resource template data)]]
          (mk-parents output)
          (spit output content))
        (-> fileset
            ((fn remove-cljs-build-files [fs]
               (core/rm fs (core/by-ext [".cljs.edn"] (core/user-files fs)))))
            (core/add-source test-dir)
            (core/add-asset asset-dir)
            (core/commit!))))))

(defn dummy-stack-trace []
  (into-array [(StackTraceElement. "BootExitCode"
                                   "DummyStackTraceElement"
                                   "DummyFile"
                                   (int 1))]))

(deftask run-cljs-test
  "Run the script produced by `cljs-test-runner` with
  cmd using the phantom_wrapper. Should be called after `boot-cljs` task."
  [c cmd CMD str "command to run to execute output js file"
   o output-path PATH str "A string representing the filepath to output test results"]
  (comp
    (serve :dir "target" :port 8989 :reload true)
    (fn middleware [next-handler]
      (fn handler [fileset]
        (-> fileset next-handler)
        (let [result (-> fileset next-handler)
              args (conj (string/split cmd #" ") "target/phantom_wrapper.js")
              [cmd & args] args]
          (conch/let-programs [cmd cmd]
            (let [results (apply cmd (concat args [{:verbose true
                                                    :throw false}]))
                  writer (if output-path
                           (io/writer output-path)
                           *out*)]
              (doseq [o (-> results :proc :out)]
                (.write writer o))
              (when output-path
                (.close writer))
              (let [exit-code @(:exit-code results)]
                (if (not= exit-code 0)
                  (throw (doto
                           (boot.App$Exit. (str exit-code))
                           (.setStackTrace (dummy-stack-trace))))))))
          result)))))
