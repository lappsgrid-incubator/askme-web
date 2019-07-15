package org.lappsgrid.eager.askme.web

import com.sun.xml.internal.org.jvnet.fastinfoset.sax.ExtendedContentHandler
import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.topic.MessageBox
import org.lappsgrid.rabbitmq.topic.PostOffice
import groovy.util.logging.Slf4j
import org.lappsgrid.eager.mining.core.json.Serializer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


/**
 *
 */
@Slf4j("logger")
class Main extends MessageBox{
    static final String BOX = 'web.mailbox'
    static final String QUERY_MBOX = 'query.mailbox'
    static final String HOST = "rabbitmq.lappsgrid.org"
    static final String EXCHANGE = "org.lappsgrid.query"
    PostOffice po = new PostOffice(EXCHANGE,HOST)


    Main(){
        super(EXCHANGE, BOX)
    }

    void dispatch(PostOffice post, String question, int id) {
        logger.info("Dispatching queries.")
        logger.debug("Sending {}", question)

        Message message = new Message().body(question).route(QUERY_MBOX).set("id", "msg$id")
        post.send(message)
        sleep(500)
    }


    void recv(Message message){

        if(message.getCommand() == 'query'){
            logger.info('Received processed question {}', message.getId())
            logger.info('Sending to solr')
            message.route('solr.mailbox')
            po.send(message)
        }
        if(message.getCommand() == 'solr'){
            logger.info('Received solr documents {}', message.getId())
            logger.info('Sending to ranking')
        }


    }
    
    void run() {
        String question = "What proteins bind to the PDGF-alpha receptor in neural stem cells?"
        int id = 1
        CountDownLatch latch = new CountDownLatch(1)
        sleep(500)
        dispatch(po, question, id)



        /*latch.await(5, TimeUnit.SECONDS)
        logger.debug "Stopping the query-sender."
        post.close()
        sleep(200)
        logger.info "sender terminating."*/

    }
    
    static void main(String[] args) {
        new Main().run()
    }
}
