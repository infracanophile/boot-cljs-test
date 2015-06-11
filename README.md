# boot-cljs-test/node-runner

```clj
[incracanophile/boot-cljs-test "0.1.0-SNAPSHOT"]
```

Forked from boot-cljs-test/node-runner project.
Boot tasks to run cljs.test in phantom/slimerjs.
Node support files are present but not currently used or usable.

Very hacked together now.

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

## Setup

```clj
(deftask cljs-auto-test []
  (comp (watch)
        (speak)
        (cljs-test-runner :namespaces '[foo.core-test bar.util-test]) ;; put it before `cljs` task
        (cljs :source-map true
              :optimizations :none)
        (run-cljs-test) ;; put it after `cljs` task
))
```

## TODO

Refactoring to get rid of hardcoded junk, other test runners, more configuration arguments for tasks.

- Nashorn runner
- Electron runner

## License

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
