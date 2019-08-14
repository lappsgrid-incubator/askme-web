package org.lappsgrid.askme.web.examples

/**
 * A Groovy template is simply a Groovy script that has methods to generate the
 * various HTML elements.
 */
class Templates {

    void html(String body) {
        println "<html>$body</html>"
    }

    void html(Closure cl) {
        //String body = cl()
        //return "<html>$body</html>"
        print "<html>"
        cl()
        println "</html>"
    }

    void body(Closure cl) {
        print "<body>"
        cl()
        print "</body>"
    }

    void p(String body) {
        print "<p>$body</p>"
    }

    void div(Map params, Closure cl) {
        String atts = params.entrySet().collect { e -> "${e.key}='${e.value}'"}.join(" ")
        print "<div $atts>"
        cl()
        print "</div>"
    }

    void run() {
        html("hello world")
        html {
            p"Goodbye cruel world"
        }

        html {
            body {
                p 'I am leaving you today.'
            }
        }

        html {
            body {
                div(class:'section', width:100) {
                    p("This is the content")
                }
                div class:'copyright', {
                    p "Copyright 2019."
                }
            }
        }
    }

    static void main(String[] args) {
        new Templates().run()
    }
}
