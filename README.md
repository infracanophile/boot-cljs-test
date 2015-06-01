# boot-cljs-test/node-runner

```clj
[incracanophile/boot-cljs-test "0.1.0"] ; NOT YET ON CLOJARS
```

Boot tasks to run cljs.test in phantom/slimerjs. Node is there but broken (from project this was forked from boot-cljs-test/node-runner project.

Very hacked together now, don't trust docs or naming yet.

## Usage

Add `infracanophile.boot-cljs-test` to your `build.boot` dependencies and
`require` the namespace:

```clj
(set-env! :dependencies '[[infracanophile/boot-cljs-test "X.Y.Z" :scope "test"]])
(require '[infracanophile.boot-cljs-test :refer [cljs-test-node-runner run-cljs-test])
```

You can see the options available on the command line:

```bash
$ boot cljs-test-node-runner -h
```

or in the REPL:

```bash
boot.user=> (doc cljs-test-node-runner)
```

## Setup

```clj
(deftask cljs-auto-test []
  (comp (watch)
        (speak)
        (cljs-test-node-runner :namespaces '[foo.core-test bar.util-test]) ;; put it before `cljs` task
        (cljs :source-map true
              :optimizations :none)
        (run-cljs-test) ;; put it after `cljs` task
))
```

## TODO

A lot of refactoring to get rid of hardcoded junk. Finish renaming "node-runner"-based names.

Nashorn runner
Electron runner

## License

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
