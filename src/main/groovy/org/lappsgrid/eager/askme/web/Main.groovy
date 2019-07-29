package org.lappsgrid.eager.askme.web

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

    void dispatch(PostOffice post, String question, int id, Map params) {

        logger.info("Dispatching question.")
        Message message = new Message()
        message.setBody(question)
        message.setRoute([QUERY_MBOX])
        message.setParameters(params)
        message.set("id", "msg$id")
        post.send(message)
    }


    void recv(Message message){

        if(message.getCommand() == 'query'){
            logger.info('Received processed question {}', message.getId())
            logger.info('Sending to solr')
            message.route(SOLR_MBOX)
            po.send(message)
        }
        else if(message.getCommand() == 'solr'){
            logger.info('Received solr documents {}', message.getId())
            rankDocuments(message)
        }
        else {
            logger.info('Received ranked document {} from question ID {}', message.getCommand(), message.getId())
            Object document = message.getBody()
            ID_doc_index."${message.getId()}".documents.add(document)
            if(ID_doc_index."${message.getId()}".count == ID_doc_index."${message.getId()}".documents.size()){
                logger.info("Query {} has all documents ({}) scored", message.getId(),ID_doc_index."${message.getId()}".count.toString())
                List sorted_documents = ID_doc_index."${message.getId()}".documents.sort {a,b -> b.score <=> a.score}
                logger.info("Query {} has all documents ({}) ranked", message.getId(),ID_doc_index."${message.getId()}".count.toString())
                //send to where?
                ID_doc_index.remove(message.getId())
            }
        }
    }

    void rankDocuments(Message message){
        Object map = message.body
        Query query = map.query
        Object documents = map.documents

        Map params = message.getParameters()
        String id = message.getId()
        logger.info('Ranking documents {}', id)
        ID_doc_index."${id}" = [:]

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
        ID_doc_index."${id}".count = document_number
        ID_doc_index."${id}".documents = []

    }


    
    void run() {
        String question1 = "What proteins bind to the PDGF-alpha receptor in neural stem cells?"
        String question2 = "What are inhibitors of Jak1?"

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
        sleep(500)
        dispatch(po, question1, id, params)
        //id = 2
        //dispatch(po, question2, id, params)

    }
    
    static void main(String[] args) {
        new Main().run()
    }
}
