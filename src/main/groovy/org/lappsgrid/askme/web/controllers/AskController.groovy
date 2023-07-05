package org.lappsgrid.askme.web.controllers

import groovy.util.logging.Slf4j
import io.micrometer.core.annotation.Timed
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.lappsgrid.askme.core.Configuration
import org.lappsgrid.askme.core.api.AskmeMessage
import org.lappsgrid.askme.core.api.Packet
import org.lappsgrid.askme.core.api.Query
import org.lappsgrid.askme.core.api.Status
import org.lappsgrid.askme.core.model.Document
import org.lappsgrid.askme.web.db.Database
import org.lappsgrid.askme.web.db.Question
import org.lappsgrid.askme.web.dto.SearchDomain
import org.lappsgrid.askme.web.services.MessageService
import org.lappsgrid.askme.web.services.PostalService
import org.lappsgrid.askme.web.util.DataCache
import org.lappsgrid.serialization.Serializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*

import javax.annotation.PostConstruct
import java.util.concurrent.TimeUnit


@Slf4j("logger")
@RestController
@ControllerAdvice
class AskController {

    private static final Configuration config = new Configuration()

    @Value('${cache.dir}')
    final String CACHE_DIR

    @Value('${cache.ttl}')
    final String CACHE_TTL

    @Value('${work.dir}')
    final String WORK_DIR

    @Value('${question.dir}')
    final String QUESTION_DIR

    @Value('${upload.postoffice}')
    final String UPLOAD_POSTOFFICE

    @Value('${upload.address}')
    final String UPLOAD_ADDRESS

    @Value('${galaxy.host}')
    final String GALAXY_HOST

    @Autowired
    MessageService messages

    @Autowired
    Database db

    @Autowired
    PostalService po

    @Autowired
    MeterRegistry registry

    DataCache cache
    File workingDir
    List<SearchDomain> cores = [
            new SearchDomain('scifact', 'SCIFACT', true),
            new SearchDomain('pubmed', 'PubMed (coming soon)', false),
            new SearchDomain('pmc', 'PubMed Central (coming soon)', false)
    ]
    Counter questionsAsked

    AskController() {
        logger.info("AskController started")
    }

    @PostConstruct
    private void init() {
        logger.info("Initializing")
        cache = new DataCache(CACHE_DIR, CACHE_TTL as Integer)
        workingDir = new File(WORK_DIR)
        if (!workingDir.exists()) {
            workingDir.mkdirs()
        }
        questionsAsked = registry.counter("questions")
    }

    @Timed(value = "post_question", percentiles = [0.5d, 0.95d])
    @CrossOrigin(origins = "http://localhost:3000")
    @PostMapping(path = "/question", produces = "application/json")
    String postQuestion(@RequestParam Map<String, String> params) {
        logger.info("POST /question?question={}", , params.question)
        //logger.info("Question : {}", params.question)
        questionsAsked.increment()
        String uuid = UUID.randomUUID()
        saveQuestion(uuid, params)

        long start = System.currentTimeMillis()
        Packet reply = answer(params, 100)
        new File("/tmp/askme-params.json").text = Serializer.toPrettyJson(params)
        new File("/tmp/askme-packet.json").text = Serializer.toPrettyJson(reply)
        long duration = System.currentTimeMillis() - start

        Map data = [:]
        data.documents = reply.documents
        data.query = reply.query
        data.duration = duration
        if (reply.documents.size() > 0) {
            Document exemplar = reply.documents[0]
            data.keys = exemplar.scores.keySet()
        }

        if (reply.status == Status.TIMEOUT || reply.status == Status.ERROR) {
            return null
        }

//        data.documents.each { Document doc ->
//            if (doc.url.contains("; ")) {
//                doc.url = getBestUrl(doc.url)
//            }
//        }

        //TODO: rebuild and see whether there are still outstanding deliveries
        //cache.add(uuid, reply)
        return Serializer.toJson(data)
    }

    private String from(List<String> candidates, String contents) {
        for (String candidate : candidates) {
            if (candidate.contains(contents)) {
                return candidate
            }
        }
        return null
    }

    private Map answer(Map params) {
        return answer(params, 100)
    }

    private void saveQuestion(String uuid, Map<String, String> data) {
        Thread.start {
            String question = data.question
            db.save(new Question(uuid, question))
            data.each { k, v ->
                if (k.contains('weight')) {
                    String name = k.replace('weight-', '')
                    db.saveSettings(uuid, name, v)
                }
            }
            File directory = new File(QUESTION_DIR)
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    logger.error("Unable to create directory {}", directory.path)
                    return
                }
            }
            File file = new File(directory, uuid + ".json")
            file.text = Serializer.toPrettyJson(data)
            logger.info("Saved question data {}", file.path)
        }
    }

    private Packet answer(Map params, int size) {
        logger.debug("Generating answer.")

        Packet result = null

        logger.trace('Constructing the message.')
        AskmeMessage message = new AskmeMessage()

//        MailBox box = new MailBox(config.EXCHANGE, message.getId(), config.HOST) {
//            @Override
//            void recv(String s) {
//                AskmeMessage response = Serializer.parse(s, AskmeMessage)
//                logger.info("Received response for {}", response.id)
//                result = response.body
//                synchronized (lock) {
//                    lock.notifyAll()
//                }
//            }
//        }

        Packet packet = new Packet()
        packet.status = Status.OK
        packet.core = params.domain
        packet.query = new Query(params.question, size)
        message.setBody(packet)
        message.setRoute([config.QUERY_MBOX, config.ELASTIC_MBOX, config.RANKING_MBOX, config.WEB_MBOX])
        message.setParameters(params)
        logger.trace('Sending the message')
        PostalService.Delivery delivery = po.send(message)
        logger.trace("Waiting for a response")
//        synchronized (lock) {
//            lock.wait(120000)
//        }
        message = delivery.get(180, TimeUnit.SECONDS) as AskmeMessage
//        result = delivery.get(60, TimeUnit.SECONDS) as Packet
        if (message == null) {
            logger.warn("Operation timed out")
//            result.error = "Operation timed out."
            result = packet
            result.status = Status.TIMEOUT
            result.message = "The server did not return a response in a timely manner."
            if (packet.documents == null) {
                packet.documents = []
            }
            return result
        }
        result = message.body
//        logger.trace('Shutting down MailBox')
//        box.close()


        /*
        // TODO We need a session managed bean so multiple users do not overwrite each other's files.
        File base = new File("/tmp/eager")
        if (!base.exists()) {
            base.mkdirs()
        }
        new File(base, 'query.json').text = Serializer.toPrettyJson(query)
        new File(base, 'files.json').text = Serializer.toPrettyJson(docs)
        new File(base, 'params.json').text = Serializer.toPrettyJson(params)
*/

//        if (result.documents.size() > size) {
//            logger.debug("Trimming results to {}", size)
//            result.documents = result.documents[0..size]
//        }
        return result
    }

    void replaceAll(List<Node> nodes, String from, String to) {
        List list = nodes.findAll { it instanceof Node && it.name() == from }
        list.each { Node node ->
            Node newNode = new Node(null, to)
            node.children().each { add(newNode, it) }
            node.replaceNode(newNode)
        }
    }

    void add(Node node, Node child) {
        node.append(child)
    }

    void add(Node node, String value) {
        node.value = value
    }

}