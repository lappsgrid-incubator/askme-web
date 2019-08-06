package org.lappsgrid.askme.web.examples

import org.junit.Test

/**
 *
 */
class DynamicBuilders {

    MetaClass makeMetaclass(Class theClass) {
        ExpandoMetaClass meta = new ExpandoMetaClass(theClass)
        meta.methodMissing = { String name, args ->
            StringWriter writer = new StringWriter()
            writer.write("<$name>")
            args.each { arg ->
                if (arg instanceof Closure) {
                    writer.write(arg())
                }
                else {
                    writer.write(arg.toString())
                }
            }
            writer.write("</$name>")
            return writer.toString()
        }

        meta.initialize()
        return meta
    }

    @Test
    void run() {
        String code = '''
html {
    body {
        p 'hello world'
        p 'hello again.'
        //p {
        //    b 'bold paragraph'
        //}
    }
}
'''
        GroovyShell shell = new GroovyShell()
        Script script = shell.parse(code)
        script.metaClass = makeMetaclass(script.class)
        println script.run()
    }

}
