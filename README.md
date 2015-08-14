# boot-cljs-test/node-runner

```clj
[incracanophile/boot-cljs-test "0.3.2"]
```

Forked from boot-cljs-test/node-runner project.
Boot tasks to run cljs.test in phantomjs.
Node support files are present but not currently used or usable.


## Usage

Add `infracanophile.boot-cljs-test` to your `build.boot` dependencies and
`require` the namespace:

```clj
(set-env! :dependencies '[[infracanophile/boot-cljs-test "X.Y.Z-SNAPSHOT" :scope "test"]])
(require '[infracanophile.boot-cljs-test :refer [cljs-test-runner run-cljs-test])
```

You can see the options available on the command line:

```bash
$ boot cljs-test-runner -h
$ boot run-cljs-test -h
```

or in the REPL:

```bash
boot.user=> (doc cljs-test-runner)
boot.user=> (doc run-cljs-test)
```

## Composing into a test runner task

```clj
(deftask cljs-test
  "Runs cljs tests once"
  [n namespaces NAMESPACES #{sym} "Symbols of namespaces to test"
   l limit-regex REGEX regex "A regex for limiting namespaces to be tested"
   t test-filters EXPR #{edn} "The set of expressions to use to filter tests"
   o output-path PATH str "A string representing the filepath to output test results"
   f formatter FORMATTER kw "Tag defining formatter to use. Accepts `junit`. Defaults to standard clojure.test output"]
  (comp (cljs-test-runner :namespaces namespaces
                          :limit-regex limit-regex
                          :test-filters test-filters
                          :formatter formatter)
        (cljs :source-map true
              :optimizations :none)
        (run-cljs-test :cmd "phantomjs --web-security false"
                       :output-path output-path)))
))
```

## TODO

Refactoring to get rid of hardcoded junk, other test runners, more configuration arguments for tasks.

- Nashorn runner
- Electron runner

## License

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
