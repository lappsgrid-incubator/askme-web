package org.lappsgrid.eager.askme.web

import com.sun.xml.internal.org.jvnet.fastinfoset.sax.ExtendedContentHandler
import org.apache.solr.common.SolrDocumentList
import org.lappsgrid.eager.mining.api.Query
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
    Map ID_doc_index = [:]
    Map params = ["title-checkbox-1" : "1",
                  "title-weight-1" : "1.0",
                  "title-checkbox-2" : "2",
                  "title-weight-2" : "1.0",
                  "title-checkbox-3" : "3",
                  "title-weight-3" : "1.0",
                  "title-checkbox-4" : "4",
                  "title-weight-4" : "1.0",
                  "title-checkbox-5" : "5",
                  "title-weight-5" : "1.0",
                  "title-checkbox-6" : "6",
                  "title-weight-6" : "1.0",
                  "title-checkbox-7" : "7",
                  "title-weight-7" : "1.0",
                  "title-weight-x" : "0.9",
                  "abstract-checkbox-1" : "1",
                  "abstract-weight-1" : "1.0",
                  "abstract-checkbox-2" : "2",
                  "abstract-weight-2" : "1.0",
                  "abstract-checkbox-3" : "3",
                  "abstract-weight-3" : "1.0",
                  "abstract-checkbox-4" : "4",
                  "abstract-weight-4" : "1.0",
                  "abstract-checkbox-5" : "5",
                  "abstract-weight-5" : "1.0",
                  "abstract-checkbox-6" : "6",
                  "abstract-weight-6" : "1.0",
                  "abstract-checkbox-7" : "7",
                  "abstract-weight-7" : "1.0",
                  "abstract-weight-x" : "1.1",
                  "domain" : "bio"]


    Main(){
        super(EXCHANGE, BOX)
    }

    void dispatch(PostOffice post, String question, int id, Map params) {

        //TO-DO: deal with params, include in message or add to
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
        else if(message.getCommand() == 'solr'){
            logger.info('Received solr documents {}', message.getId())
            rankDocuments(message)
        }
        else if(message.getId() in ID_doc_index){
            logger.info('Received ranked document {} from question ID {}', message.getCommand(), message.getId())
        }

    }

    void rankDocuments(Message message){
        Object map = message.body
        Query query = map.query
        Object documents = map.documents

        String id = message.getId()
        logger.info('Ranking documents {}', id)
        ID_doc_index."${id}" = map

        Integer dn = 0
        documents.each{document ->
            logger.info('Preparing to send document {} from query {}',dn,id)
            Map m = [:]
            m.query = query
            m.document = document
            Message q = new Message(dn.toString(), m, params, 'ranking.mailbox')
            q.setId(id)
            po.send(q)
            logger.info('Sent document {} from query {} to be ranked.', dn, id)
            dn+=1
        }
    }


    
    void run() {
        String question = "What proteins bind to the PDGF-alpha receptor in neural stem cells?"
        Map params = ["title-checkbox-1" : "1",
        "title-weight-1" : "1.0",
        "title-checkbox-2" : "2",
        "title-weight-2" : "1.0",
        "title-checkbox-3" : "3",
        "title-weight-3" : "1.0",
        "title-checkbox-4" : "4",
        "title-weight-4" : "1.0",
        "title-checkbox-5" : "5",
        "title-weight-5" : "1.0",
        "title-checkbox-6" : "6",
        "title-weight-6" : "1.0",
        "title-checkbox-7" : "7",
        "title-weight-7" : "1.0",
        "title-weight-x" : "0.9",
        "abstract-checkbox-1" : "1",
        "abstract-weight-1" : "1.0",
        "abstract-checkbox-2" : "2",
        "abstract-weight-2" : "1.0",
        "abstract-checkbox-3" : "3",
        "abstract-weight-3" : "1.0",
        "abstract-checkbox-4" : "4",
        "abstract-weight-4" : "1.0",
        "abstract-checkbox-5" : "5",
        "abstract-weight-5" : "1.0",
        "abstract-checkbox-6" : "6",
        "abstract-weight-6" : "1.0",
        "abstract-checkbox-7" : "7",
        "abstract-weight-7" : "1.0",
        "abstract-weight-x" : "1.1",
        "domain" : "bio"]

        int id = 1
        CountDownLatch latch = new CountDownLatch(1)
        sleep(500)
        dispatch(po, question, id, params)



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
