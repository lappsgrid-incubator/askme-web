package org.lappsgrid.askme.web.examples

/**
 *
 */
class Templates {

    String html(String body) {
        return "<html>$body</html>"
    }

    String html(Closure cl) {
        String body = cl()
        return "<html>$body</html>"
    }

    String body(Closure cl) {
        return "<body>${cl()}</body>"
    }

    String p(String body) {
        return "<p>$body</p>"
    }

    void run() {
        println html("hello world")
        println html {
            "Goodbye cruel world"
        }

        println html {
            body {
                p 'I am leaving you today.'
            }
        }
    }

    static void main(String[] args) {
        new Templates().run()
    }
}
