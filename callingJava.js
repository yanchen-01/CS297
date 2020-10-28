const spawn = require("child_process").spawnSync;
module.exports = class CallingJava {
    constructor() {
        spawn("javac", ["msg/MSG.java"]);
        let ls = spawn("java", ["msg/MSG", "base"]);
        // eslint-disable-next-line radix
        this.baseScore = parseInt(ls.stdout);
        this.getWidth();
    }

    alignScore(alignment) {
        let ls = spawn("java", ["msg/MSG", "score", alignment]);
        // eslint-disable-next-line radix
        return parseInt(ls.stdout);
    }

    align(width) {
        let ls = spawn("java", ["msg/MSG", "align", width]);
        // eslint-disable-next-line radix
        return ls.stdout.toString();
    }

    getWidth() {
        let ls = spawn("java", ["msg/MSG", "widths"]);
        let widthString = ls.stdout.toString();
        widthString = widthString.replace('[', "");
        widthString = widthString.replace(']', "");
        this.widths = widthString.split`,`.map(x => +x);
    }
};