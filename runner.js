var p = require('webpage').create();
var sys = require('system');
p.onConsoleMessage = function (x) {
  var line = x;
  if (line !== "[NEWLINE]") {
    console.log(line.replace(/\[NEWLINE\]/g, "\n"));
  }
};
p.injectJs(sys.args[1]);
phantom.exit(0);