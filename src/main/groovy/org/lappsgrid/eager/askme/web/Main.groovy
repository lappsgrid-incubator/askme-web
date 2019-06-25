package org.lappsgrid.eager.askme.web

import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.topic.PostOffice
import groovy.util.logging.Slf4j

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


/**
 *
 */
@Slf4j("logger")
class Main {
    static final String BOX = 'query sender'

    void dispatch(PostOffice post, String question) {
        logger.info("Dispatching queries.")
        logger.debug("Sending {}", question)
        //print("Dispatching queries")
        //print("Sending...")

        Message message = new Message().body(question).route('query receiver')
        post.send(message)
        //print('Message Sent')
        sleep(500)

        logger.debug("Dispatched question")
    }
    
    void run() {
        //SimpleQueryProcessor processor = new SimpleQueryProcessor()
        //Query query = processor.transform("What proteins bind to the PDGF-alpha receptor in neural stem cells?")
        //println query.query
        String question = "What proteins bind to the PDGF-alpha receptor in neural stem cells?"

        PostOffice post = new PostOffice('askme.prototype', 'rabbitmq.lappsgrid.org')
        CountDownLatch latch = new CountDownLatch(1)
        sleep(500)
        dispatch(post, question)

        latch.await(5, TimeUnit.SECONDS)
        logger.debug "Stopping the query-sender."
        post.close()
        sleep(200)
        logger.info "sender terminating."

    }
    
    static void main(String[] args) {
        new Main().run()
    }
}
