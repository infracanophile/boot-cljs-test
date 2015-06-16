(ns infracanophile.boot-cljs-test
  (:import [boot App]
           [java.lang StackTraceElement])
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
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

(deftask cljs-test-runner
  "Automatically produces:

  - a Clojurescript source that will run all tests in given `namespaces` list.
  - an EDN file to instruct `boot-cljs` to build the file targeting the browser.

  Should be called before `boot-cljs` task."
  [n namespaces NS #{sym} "The set of namespace symbols to run test in."
   r regex REGEX str "The set of expressions to use to filter namespaces."]
  (when (and namespaces regex)
    (throw (Exception. "cljs-test-runner: Either list namespaces or provide regex, not both")))
  (let [test-command (if namespaces
                       "run-tests"
                       "run-all-tests")
        required-namespaces (if namespaces
                              (required-ns namespaces))
        tested-namespaces (if namespaces
                            (tested-ns namespaces))
        data {:required-ns required-namespaces
              :tested-ns tested-namespaces
              :test-regex regex}
        templates {:sources
                   ["infracanophile/boot_cljs_test/phantom_runner.cljs"
                    "cljs_test_phantom_runner.cljs.edn"]
                   :assets
                   ["phantom_wrapper.js"
                    "phantom_wrapper.js.html"]}
        test-dir (core/tmp-dir!)
        asset-dir (core/tmp-dir!)]
    (core/with-pre-wrap fileset
      (file/empty-dir! test-dir)
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
          (core/add-source test-dir)
          (core/add-asset asset-dir)
          (core/commit!)))))

(defn dummy-stack-trace []
  (into-array [(StackTraceElement. "BootExitCode"
                                   "DummyStackTraceElement"
                                   "DummyFile"
                                   (int 1))]))

(deftask run-cljs-test
  "Run the script produced by `cljs-test-runner` with
  cmd using the phantom_wrapper. Should be called after `boot-cljs` task."
  [c cmd str "command to run to execute output js file"]
  (comp
    (serve :dir "target" :port 8989 :reload true)
    (fn middleware [next-handler]
      (fn handler [fileset]
        (-> fileset next-handler)
        (let [cmds (conj (string/split cmd #" ") "target/phantom_wrapper.js")
              exit-code ((apply sh cmds))]
          (if (not= exit-code 0)
            (throw (doto
                     (boot.App$Exit. (str exit-code))
                     (.setStackTrace (dummy-stack-trace))))))))))
