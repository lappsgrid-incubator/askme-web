package org.lappsgrid.askme.web

import org.apache.solr.common.SolrDocument
import org.lappsgrid.eager.mining.api.Query

import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.topic.MessageBox
import org.lappsgrid.rabbitmq.topic.PostOffice
import groovy.util.logging.Slf4j


/**
 *
 */
@Slf4j("logger")
class Main extends MessageBox{
    static final String MBOX = 'web.mailbox'
    static final String QUERY_MBOX = 'query.mailbox'
    static final String SOLR_MBOX = 'solr.mailbox'
    static final String RANKING_MBOX = 'ranking.mailbox'
    static final String HOST = "rabbitmq.lappsgrid.org"
    static final String EXCHANGE = "org.lappsgrid.query"
    static final PostOffice po = new PostOffice(EXCHANGE,HOST)
    Map ID_doc_index = [:]


    Main(){
        super(EXCHANGE, MBOX)
    }

    void dispatch(PostOffice post, String question, int id, Map params, int number_of_documents) {

        logger.info("Dispatching question.")
        Message message = new Message()
        message.setBody(question)
        message.setRoute([QUERY_MBOX])
        message.setParameters(params)
        message.set("id", "msg$id")
        post.send(message)
        setupIDIndex(message, number_of_documents)
    }

    void setupIDIndex(Message message, int number_of_documents){
        //ID_doc_index."${message.getId()}" = [:]
        //ID_doc_index."${message.getId()}".count = number_of_documents
        String id = message.getId()
        ID_doc_index[id] = [:]
        ID_doc_index[id].count = number_of_documents
    }


    void recv(Message message){

        String command = message.getCommand()
        String id = message.getId()
        Object body = message.getBody()



        if(command == 'EXIT' || command == 'QUIT') {
            shutdown()
        }
        else if(command == 'ERROR'){
            logger.info('Received Error Message: {}', body)
        }
        else if(command == 'query'){
            logger.info('Received processed question {}', id)
            logger.info('Sending to solr')
            //message.setCommand(ID_doc_index."${message.getId()}".count.toString())
            message.setCommand(ID_doc_index[id].count.toString())
            message.route(SOLR_MBOX)
            po.send(message)
        }
        else if(command == 'solr'){
            logger.info('Received solr documents {}',id)
            rankDocuments(message)
        }
        else {
            logger.info('Received ranked document {} from question ID {}', command, id)
            Object document = body

            //ID_doc_index."${message.getId()}".documents.add(document)
            ID_doc_index[id].documents.add(document)

            //if(ID_doc_index."${message.getId()}".count == ID_doc_index."${message.getId()}".documents.size()){
            if(ID_doc_index[id].count == ID_doc_index[id].documents.size()){
                //logger.info("Query {} has all documents ({}) scored", id,ID_doc_index."${message.getId()}".count.toString())
                logger.info("Query {} has all documents ({}) scored", id,ID_doc_index[id].count.toString())

                Map results = [:]

                //List sorted_documents = ID_doc_index."${message.getId()}".documents.sort {a,b -> b.score <=> a.score}
                List sorted_documents = ID_doc_index[id].documents.sort {a,b -> b.score <=> a.score}

                //int n = ID_doc_index."${message.getId()}".count
                int n = ID_doc_index[id].count
                //Query query = ID_doc_index."${message.getId()}".query
                Query query = ID_doc_index[id].query

                results.query = query
                results.documents = sorted_documents
                results.size = n

                //logger.info("Query {} has all documents ({}) ranked", message.getId(),ID_doc_index."${message.getId()}".count.toString())
                logger.info("Query {} has all documents ({}) ranked", id,ID_doc_index[id].count.toString())

                //ID_doc_index.remove(message.getId())
                ID_doc_index.remove(id)
                Message remove_ranking_processor = new Message()
                remove_ranking_processor.setRoute([RANKING_MBOX])
                remove_ranking_processor.setCommand('remove_ranking_processor')
                remove_ranking_processor.setId(id)
                logger.info("Removing ranking processor {}", id)
                po.send(remove_ranking_processor)
            }
            send_shutdown()

        }
    }

    void rankDocuments(Message message){
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
            Message q = new Message(document_number.toString(), m, params, RANKING_MBOX)
            q.setId(id)
            po.send(q)
            logger.info('Sent document {} from query {} to be ranked.', document_number, id)
        }
        //ID_doc_index."${id}".query = query
        //ID_doc_index."${id}".documents = []
        ID_doc_index[id].query = query
        ID_doc_index[id].documents = []

    }
    void send_shutdown(){
        Message shutdown_message = new Message()
        shutdown_message.setCommand('EXIT')
        List<String> recip = [QUERY_MBOX,SOLR_MBOX,RANKING_MBOX,MBOX]
        recip.each{client ->
            shutdown_message.setRoute([client])
            po.send(shutdown_message)
        }
    }
    void shutdown(){
        logger.info('Received shutdown message, terminating Web service')
        po.close()
        logger.info('Web service terminated')
        System.exit(0)
    }


    
    void run() {

        String question1 = "What proteins bind to the PDGF-alpha receptor in neural stem cells?"
        //String question2 = "What are inhibitors of Jak1?"

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
        int number_of_documents = 1
        sleep(500)
        dispatch(po, question1, id, params, number_of_documents)
        //id = 2
        //dispatch(po, question2, id, params, number_of_documents)
        //testErrorService()

    }
    void testErrorService(){
        /*
        Message query_test = new Message()
        query_test.setRoute([QUERY_MBOX])
        po.send(query_test)

        Message solr_test = new Message()
        solr_test.setRoute([SOLR_MBOX])
        solr_test.setBody('not empty')
        po.send(solr_test)

         */
        Message ranking_test = new Message()
        ranking_test.setRoute([RANKING_MBOX])
        Map k = [:]
        k.document = new SolrDocument()
        k.query = new Query()
        ranking_test.setBody(k)
        po.send(ranking_test)
    }
    
    static void main(String[] args) {
        new Main().run()
    }
}
