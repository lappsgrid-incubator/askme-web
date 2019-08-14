package org.lappsgrid.askme.web.examples

import org.junit.Test

/**
 * Each class in Groovy has
 */
class MetaProgramming {

    /*
     * Define a custom MetaClass with a fixed set of new methods.
     */
    MetaClass makeMetaclass(Class theClass) {
        ExpandoMetaClass meta = new ExpandoMetaClass(theClass)
        meta.html = { Closure cl ->
            return "<html>${cl()}</html>"
        }
        meta.body = { Closure cl ->
            return "<body>${cl()}</body>"
        }
        meta.p = { String body ->
            return "<p>$body</p>"
        }
        meta.initialize()

        return meta
    }

    @Test
    void withMetaclass() {
        String code = '''
html {
    body {
        p 'Hello MetaClass.'
    }
}
'''
        GroovyShell shell = new GroovyShell()
        Script script = shell.parse(code)
        script.metaClass = makeMetaclass(script.class)
        println script.run()
    }

    @Test
    void metaProgramming() {
        String.metaClass.greet {
            println "Hello $delegate"
        }
        String s = "world"
        s.greet()
    }

}
