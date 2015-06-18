(ns infracanophile.boot-cljs-test.phantom-runner
  (:require [cljs.analyzer.api :as ana-api]))

(defmacro test-all-vars-block
  [pred [quote n]]
  `(let [env# (cljs.test/get-current-env)]
     (concat
       [(fn []
          (when (nil? env#)
            (cljs.test/set-env! (cljs.test/empty-env)))
          ~(when (ana-api/ns-resolve n 'cljs-test-once-fixtures)
             `(cljs.test/update-current-env! [:once-fixtures] assoc '~n
                                             ~(symbol (name n) "cljs-test-once-fixtures")))
          ~(when (ana-api/ns-resolve n 'cljs-test-each-fixtures)
             `(cljs.test/update-current-env! [:each-fixtures] assoc '~n
                                             ~(symbol (name n) "cljs-test-each-fixtures"))))]
       (cljs.test/test-vars-block
         [~@(map
              (fn [[k _]]
                `(var ~(symbol (name n) (name k))))
              (filter
                (fn [[_ v]] (and (:test v) ((eval pred) v)))
                (ana-api/ns-interns n)))])
       [(fn []
          (when (nil? env#)
            (cljs.test/clear-env!)))])))

(defmacro test-ns-block
  ([env pred [quote ns :as form]]
   (assert (ana-api/find-ns ns) (str "Namespace " ns " does not exist"))
   `[(fn []
       (cljs.test/set-env! ~env)
       (cljs.test/do-report {:type :begin-test-ns, :ns ~form})
       ;; If the namespace has a test-ns-hook function, call that:
       ~(if-let [v (ana-api/ns-resolve ns 'test-ns-hook)]
          `(~(symbol (name ns) "test-ns-hook"))
          ;; Otherwise, just test every var in the namespace.
          `(cljs.test/block (test-all-vars-block ~pred ~form))))
     (fn []
       (cljs.test/do-report {:type :end-test-ns, :ns ~form}))]))

(defmacro run-tests-block
  "Like test-vars, but returns a block for further composition and
  later execution."
  [env pred & namespaces]
  (let [summary (gensym "summary")]
    `(let [~summary (cljs.core/volatile!
                      {:test 0 :pass 0 :fail 0 :error 0
                       :type :summary})]
       (concat ~@(map
                   (fn [ns]
                     `(concat (test-ns-block ~env ~pred ~ns)
                              [(fn []
                                 (cljs.core/vswap!
                                   ~summary
                                   (partial merge-with +)
                                   (:report-counters
                                     (cljs.test/get-and-clear-env!))))]))
                   namespaces)
               [(fn []
                  (cljs.test/set-env! ~env)
                  (cljs.test/do-report (deref ~summary))
                  (cljs.test/report (assoc (deref ~summary) :type :end-run-tests))
                  (cljs.test/clear-env!))]))))

(defmacro run-tests
  [env pred & namespaces]
  `(cljs.test/run-block (run-tests-block ~env ~pred ~@namespaces)))
