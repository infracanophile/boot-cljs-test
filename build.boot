(set-env!
  :resource-paths #{"resources"}
  :source-paths   #{"src"}
  :dependencies   '[[org.clojure/tools.reader "1.0.0-alpha1"]
                    [org.clojure/tools.namespace "0.3.0-alpha2"
                     :exclusions [org.clojure/tools.reader]]
                    [de.ubercode.clostache/clostache "1.4.0"]
                    [me.raynes/conch "0.8.0"]
                    [pandeiro/boot-http "0.6.3"]
                    [adzerk/bootlaces "0.1.10" :scope "test"]])

(require '[adzerk.bootlaces :refer :all])

(def +version+ "0.3.8")

(bootlaces! +version+)

(task-options!
 pom {:project     'infracanophile/boot-cljs-test
      :version     +version+
      :description "Boot tasks to produce a script to test ClojureScript and to run this script"
      :url         "https://github.com/infracanophile/boot-cljs-test"
      :scm         {:url "https://github.com/infracanophile/boot-cljs-test"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}})
