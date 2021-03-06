package org.lappsgrid.askme.web

import org.lappsgrid.askme.core.Configuration
import org.lappsgrid.askme.core.api.Query
import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.topic.MailBox
import org.lappsgrid.rabbitmq.topic.PostOffice
import groovy.util.logging.Slf4j
import org.lappsgrid.serialization.Serializer

/**
 * TODO:
 * 1) Update imports to phase out eager (waiting on askme-core pom)
 * 2) Add exceptions / case statements to recv method?
 * 3) Integrate with groovy html templates
 */

@Slf4j("logger")
class Main {
    static final String MBOX = 'web.mailbox'
    static final String QUERY_MBOX = 'query.mailbox'
    static final String SOLR_MBOX = 'solr.mailbox'
    static final String RANKING_MBOX = 'ranking.mailbox'

    static final Configuration config = new Configuration()

    static final PostOffice po = new PostOffice(config.EXCHANGE,config.HOST)
    MailBox box

    Main(){
    }

    void run(Object lock) {
        println 'Running main'
        //General setup, and dispatching of example question
        String question = "What proteins bind to the PDGF-alpha receptor in neural stem cells"
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
        int temp_id = 1
        int number_of_documents = 2
//        sleep(500)
        //Need to return ID, to update ID_doc_index count parameter
//        String ident = dispatch(po, question, temp_id, params)
        Map ID_doc_index = [:]
//        ID_doc_index[ident] = [:]
//        ID_doc_index[ident].count = number_of_documents


        box = new MailBox(config.EXCHANGE, MBOX, config.HOST) {
            @Override
            void recv(String s){
                println "Received a message: $s"
                Message message = Serializer.parse(s, Message)
                String command = message.getCommand()
                String id = message.getId()
                Object body = message.getBody()

                if(command == 'EXIT' || command == 'QUIT') {
                    logger.info('Received shutdown message, terminating Web service')
                    synchronized(lock) { lock.notify() }
                }
                else if(command == 'PING'){
                    logger.info('Received PING message from {}', body)
                    message.command = 'PONG'
                    message.body = 'PONG'
                    Main.this.po.send(message)
                }
                else if (command == 'ask') {
                    logger.info "Asking question"
                    String ident = dispatch(question, params)
                    ID_doc_index[ident] = [:]
//                    ID_doc_index[ident].count = number_of_documents
                }
                else if(command == 'query'){
                    logger.info('Received processed question {}', id)
                    logger.info('Sending to solr')
                    message.setCommand(ID_doc_index[id].count.toString())
                    message.route(SOLR_MBOX)
                    Main.this.po.send(message)
                }
                else if(command == 'solr'){
                    logger.info('Received solr documents {}',id)
                    rankDocuments(message, ID_doc_index)
                }
                else {
                    logger.info('Received ranked document {} from question ID {}', command, id)
                    Object document = body
                    ID_doc_index[id].documents.add(document)
                    if(ID_doc_index[id].count == ID_doc_index[id].documents.size()){
                        logger.info("Query {} has all documents ({}) scored", id,ID_doc_index[id].count.toString())
                        Map results = [:]
                        List sorted_documents = ID_doc_index[id].documents.sort {a,b -> b.score <=> a.score}
                        int n = ID_doc_index[id].count
                        Query query = ID_doc_index[id].query
                        results.query = query
                        results.documents = sorted_documents
                        results.size = n
                        logger.info("Query {} has all documents ({}) ranked", id,ID_doc_index[id].count.toString())
                        ID_doc_index.remove(id)
                        Message remove_ranking_processor = new Message()
                        remove_ranking_processor.setRoute([RANKING_MBOX])
                        remove_ranking_processor.setCommand('remove_ranking_processor')
                        remove_ranking_processor.setId(id)
                        logger.info("Removing ranking processor {}", id)
                        Main.this.po.send(remove_ranking_processor)

                        //Only tested here because of one question
                        test_ping_pong()
                        sleep(5000)
                        send_shutdown()
                    }
                }
            }
        }
        synchronized(lock) { lock.wait() }
        box.close()
        po.close()
        logger.info("Web service terminated")
    }
    
    static void main(String[] args) {
        logger.info("Starting Web service")
        Object lock = new Object()
        Thread.start {
            new Main().run(lock)
        }
    }

    String dispatch(String question, Map params) {
        logger.info("Dispatching question.")
        Message message = new Message()
        message.setBody(question)
        message.setRoute([QUERY_MBOX])
        message.setParameters(params)
        message.set("id", UUID.randomUUID().toString())
        po.send(message)
        return message.getId()
    }

    void rankDocuments(Message message, Map ID_doc_index){
        Object map = message.body
        Query query = map.query
        Object documents = map.documents
        Map params = message.getParameters()
        String id = message.getId()
        logger.info('Ranking documents {}', id)
        int document_number = 0
        documents.each{document ->
            document_number+=1
            logger.info('Preparing to send document {} from Message {}',document_number,id)
            Map m = [:]
            m.query = query
            m.document = document
            Message to_ranking = new Message()
            to_ranking.setId(id)
            to_ranking.setParameters(params)
            to_ranking.setRoute([RANKING_MBOX])
            to_ranking.setBody(m)
            po.send(to_ranking)
            logger.info('Sent document {} from query {} to be ranked.', document_number, id)
        }
        ID_doc_index[id].query = query
        ID_doc_index[id].documents = []
    }

    //Tests for shutdown and PING PONG
    void send_shutdown(){
        Message shutdown_message = new Message()
        shutdown_message.setCommand('EXIT')
        List<String> recip = [QUERY_MBOX,SOLR_MBOX,RANKING_MBOX,MBOX]
        recip.each{client ->
            shutdown_message.setRoute([client])
            po.send(shutdown_message)
        }
    }
    void test_ping_pong(){
        Message ping = new Message()
        ping.setCommand('PING')
        ping.setBody(MBOX)
        List<String> recip = [QUERY_MBOX,SOLR_MBOX,RANKING_MBOX]
        recip.each{client ->
            ping.setRoute([client])
            po.send(ping)
        }
    }




    /**
     void testErrorService(){
     /*
     Message query_test = new Message()
     query_test.setRoute([QUERY_MBOX])
     po.send(query_test)

     Message solr_test = new Message()
     solr_test.setRoute([SOLR_MBOX])
     solr_test.setBody('not empty')
     po.send(solr_test)


     Message ranking_test = new Message()
     ranking_test.setRoute([RANKING_MBOX])
     Map k = [:]
     k.document = new SolrDocument()
     k.query = new Query()
     ranking_test.setBody(k)
     po.send(ranking_test)
     }


     void setupIDIndex(Message message, int number_of_documents){
     String id = message.getId()
     ID_doc_index[id] = [:]
     ID_doc_index[id].count = number_of_documents
     //logger.info(ID_doc_index.toString())
     }


     */

}
