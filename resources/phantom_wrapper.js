var p = require('webpage').create();
var sys = require('system');
var fs = require('fs');

var pagePath = "../" + sys.args[0] + ".html";

var html = "<html><head><script src=\"http://localhost:8989/target/cljs_test_phantom_runner.js\"></script></head><body></body></html>";

fs.write(pagePath, html, 'w');

p.onConsoleMessage = function(msg) {
    var exit = msg.replace("phantom-exit-code:", "");
    if (msg != exit) {
        if (p) p.close();
        setTimeout(function(){phantom.exit(parseInt(exit)); }, 0);
        phantom.onError = function(){};
        throw new Error('');
    }
    console.log(msg);
}

p.open("http://localhost:8989/phantom_wrapper.js.html", function() {
    p.evaluate(function() {
        infracanophile.boot_cljs_test.phantom_runner.main();
    });
});
