var p = require('webpage').create();

p.onConsoleMessage = function(msg) {
    console.log(msg);
}

p.open("localhost:8989/target/cljs_test_phantom_runner.js", function() {
    console.log("page opened");
});
