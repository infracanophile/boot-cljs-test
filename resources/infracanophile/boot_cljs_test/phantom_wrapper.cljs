(let [p (js/require "webpage")
      p (.create p)]
  (.onConsoleMessage p (fn [msg] (.log js/console msg)))
  (.open p
         "localhost:8989/target/cljs_test_phantom_runner.js"
         (fn [] (.log js/console "page opened"))))
