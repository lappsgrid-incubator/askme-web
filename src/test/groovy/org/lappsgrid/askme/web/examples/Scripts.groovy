package org.lappsgrid.askme.web.examples

import org.junit.Ignore
import org.junit.Test

/**
 *
 */
@Ignore
class Scripts {

    @Test
    void run() {
        String code = "println 'hello world.'"

        GroovyShell shell = new GroovyShell()
        Script script = shell.parse(code)
        script.run()
    }

    @Test
    void injectVariables() {
        String code = 'println "hello $who."'

        Binding binding = new Binding()
        binding.setVariable("who", "injected variable")
        GroovyShell shell = new GroovyShell(binding)
        Script script = shell.parse(code)
        script.run()
    }

    @Test
    void danger_danger() {
        // Why we don't execute arbitray code sent from users...
        String code = 'println new File("/etc/passwd").text'

        GroovyShell shell = new GroovyShell()
        Script script = shell.parse(code)
        script.run()
        // We just leak our systems password file...
    }
}
