package org.lappsgrid.askme.web.examples

import org.junit.Test

/**
 *
 */
class DynamicBuilders {

    StringWriter writer

    MetaClass makeMetaclass(Class theClass) {
        ExpandoMetaClass meta = new ExpandoMetaClass(theClass)
        meta.methodMissing = { String name, args ->
            writer.write("<$name>")
            args.eachWithIndex { arg, i ->
                if (arg instanceof Closure) {
                    arg()
                }
                else {
                    writer.write(arg.toString())
                }
            }
            writer.write("</$name>")
        }

        meta.initialize()
        return meta
    }

    @Test
    void run() {
        execute '''
html {
    body {
        p 'hello world'
        p 'hello again.'
        p {
            b '"bold" paragraph'
        }
    }
}
'''
    }

    @Test
    void binding() {
        Binding binding = new Binding()
        binding.setVariable("name", "world")
        execute binding, '''
html {
    body {
        p 'hello world'
        p "hello $name"
        p {
            b '"bold" paragraph'
        }
    }
}
'''
    }

    @Test
    void shared_binding() {
        Binding binding = new Binding()
        binding.setVariable("name", "Kevin")
        execute binding, '''
html {
    body {
        p 'hello world'
        p "hello $name"
        // Change the value of the variable in the Binding object.
        name = "Keith"
    }
}
'''
        String name = binding.getVariable("name")
        println "Name in binding: $name"

        execute binding, '''
p "Using the binding set in the first script: $name"
'''
    }


    @Test
    void xml() {
        execute '''
xml {
    foo {
        bar "Hello world"
        bax "hello again"
        bax {
            x "world"
            
        }
    }
}
'''
    }

    @Test
    void missing_method() {
        String.metaClass.methodMissing = { name, args ->
            if (name == 'greet') {
                println "Hello ${delegate}."
            }
            else {
                println "Intercepted missing method $name"
            }
        }
        String s = "world"
        s.greet()
        s.foobar()
    }

    void execute(String code) {
        execute(new Binding(), code)
    }

    void execute(Binding binding, String code) {
        writer = new StringWriter()
        GroovyShell shell = new GroovyShell(binding)
        Script script = shell.parse(code)
        script.metaClass = makeMetaclass(script.class)
        script.run()
        println writer.toString()
        writer = null
    }
}

