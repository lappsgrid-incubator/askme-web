package org.lappsgrid.askme.web

import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.lappsgrid.askme.core.Configuration
import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.topic.MessageBox
import org.lappsgrid.rabbitmq.topic.PostOffice

/**
 *
 */
class MainTest {
    static Configuration config
    Main app
    Object lock
    final String MAILBOX = "web-test-mailbox"

    @BeforeClass
    static void init() {
        config = new Configuration()
    }


    @Before
    void setup() {
        lock = new Object()
        app = new Main()
        Thread.start {
            println "Running the app"
            app.run(lock)
        }
        println "Setup complete."
    }

    @After
    void teardown() {
//        app.stop()
        app = null
    }

    @Test
    void ping() {
        boolean passed = false
        println "Creating the return mailbox."
        MessageBox box = new MessageBox(config.EXCHANGE, MAILBOX, config.HOST) {

            @Override
            void recv(Message message) {
                passed = message.command == "PONG"
                synchronized (lock) {
                    lock.notifyAll()
                }
            }
        }

        println "Opening the post office."
        PostOffice po = new PostOffice(config.EXCHANGE, config.HOST)
        println "creating the message"
        Message message = new Message()
                .command("PING")
                .route(Main.MBOX)
                .route(MAILBOX)
        sleep(500)
        println "Sending the message"
        po.send(message)
        println "Waiting for the lock"
        synchronized (lock) {
            try {
                lock.wait(5000)
                println "Wait done."
            }
            catch (InterruptedException e) {
                println "Wait was interrupted."
            }
        }
        println "Closing post office and mailbox"
        po.close()
        box.close()
        assert passed
    }
}
