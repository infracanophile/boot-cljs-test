(ns infracanophile.boot-cljs-test
  (:require [clojure.java.io :as io]
            [boot.core :as core :refer [deftask]]
            [boot.util :as util :refer [sh]]
            [boot.file :as file]
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
  [n namespaces NS #{sym} "Namespaces whose tests will be run."]
  (let [templates {:sources
                   ["infracanophile/boot_cljs_test/phantom_runner.cljs"
                    "cljs_test_phantom_runner.cljs.edn"]
                   :assets
                   ["phantom_wrapper.js"]}
        test-dir (core/tmp-dir!)
        asset-dir (core/tmp-dir!)]
    (core/with-pre-wrap fileset
      (file/empty-dir! test-dir)
      (doseq [template (:sources templates)
              :let [data {:required-ns (required-ns namespaces)
                          :tested-ns (tested-ns namespaces)}
                    output (io/file test-dir template)
                    content (render-resource template data)]]
        (mk-parents output)
        (spit output content))
      (file/empty-dir! asset-dir)
      (doseq [template (:assets templates)
              :let [data {:required-ns (required-ns namespaces)
                          :tested-ns (tested-ns namespaces)}
                    output (io/file asset-dir template)
                    content (render-resource template data)]]
        (mk-parents output)
        (spit output content))
      (-> fileset
          (core/add-source test-dir)
          (core/add-asset asset-dir)
          (core/commit!)))))

(deftask run-cljs-test
  "Run the script produced by `cljs-test-runner` with
  cmd using the phantom_wrappe. Should be called after `boot-cljs` task."
  [c cmd str "command to run to execute output js file"]
  (fn middleware [next-handler]
    (fn handler [fileset]
      (sh cmd "target/phantom_wrapper.js")
      (-> fileset next-handler))))
