(ns infracanophile.boot-cljs-test.phantom-runner
  (:require-macros [cljs.test :as t]
                   [infracanophile.boot-cljs-test.phantom-runner :refer [run-tests]])
  (:require
   [cljs.test :as t :refer [report]]
   {{required-ns}}))

(enable-console-print!)

(defmethod report [:cljs.test/default :summary] [m]
  (println "\nRan " (:test m) " tests containing")
  (println (+ (:pass m) (:fail m) (:error m)) " assertions.")
  (println (:fail m) " failures, " (:error m) " errors."))

(defmethod report [:cljs.test/default :end-run-tests] [m]
  (println "phantom-exit-code:" (if (t/successful? m) 0 1)))

(defn main []
  (run-tests
      (t/empty-env :cljs.test/default)
      {{test-predicate}}
      {{tested-ns}}))
