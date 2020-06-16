package org.lappsgrid.askme.web.examples

import org.junit.Ignore
import org.junit.Test

/**
 * Groovy provides a GroovyShell object which can be used to compile other Groovy
 * code at run time.
 *
 * Being able to easily run Strings as Groovy code is very powerful, but also
 * very dangerous.
 */
@Ignore
class Scripts {

    @Test
    void hello_world() {
        String code = "println 'hello world.'"

        GroovyShell shell = new GroovyShell()
        Script script = shell.parse(code)
        script.run()
    }

    @Test
    void bindings() {
        String code = 'println "hello $who."'

        Binding binding = new Binding()
        binding.setVariable("who", "injected variable")
        GroovyShell shell = new GroovyShell(binding)
        Script script = shell.parse(code)
        script.run()
    }

    @Test
    void recursiveCompilation() {
        String code =  '''
    String more_code = 'println "hello $x."'
    Binding binding = new Binding()
    binding.setVariable('x', 'world')
    GroovyShell shell = new GroovyShell(binding)
    Script script = shell.parse(more_code)
    println "Running compiled script"
    script.run()
'''
        GroovyShell shell = new GroovyShell()
        Script script = shell.parse(code)
        println "Running main script."
        script.run()
    }

    @Test
    void danger_danger() {
        // Why we don't execute arbitrary code sent from users...
        String code = 'println new File("/etc/passwd").text'

        GroovyShell shell = new GroovyShell()
        Script script = shell.parse(code)
        script.run()
        // Did we just leak our system's password file?
    }

}
