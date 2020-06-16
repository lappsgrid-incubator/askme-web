package org.lappsgrid.askme.web.controllers

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import io.micrometer.core.annotation.Timed
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.lappsgrid.askme.core.Configuration
import org.lappsgrid.askme.core.Utils
import org.lappsgrid.askme.core.api.AskmeMessage
import org.lappsgrid.askme.core.api.Packet
import org.lappsgrid.askme.core.api.Query
import org.lappsgrid.askme.core.api.Status
import org.lappsgrid.askme.core.concurrent.Signal
import org.lappsgrid.askme.core.model.Document
import org.lappsgrid.askme.web.Version
import org.lappsgrid.askme.web.db.Database
import org.lappsgrid.askme.web.db.Question
import org.lappsgrid.askme.web.dto.SearchDomain
import org.lappsgrid.askme.web.services.MessageService
import org.lappsgrid.askme.web.services.PostalService
import org.lappsgrid.askme.web.util.DataCache
import org.lappsgrid.discriminator.Discriminators
import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.topic.MailBox
import org.lappsgrid.rabbitmq.topic.PostOffice
import org.lappsgrid.serialization.Data
import org.lappsgrid.serialization.Serializer
import org.lappsgrid.serialization.lif.Container
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

import javax.annotation.PostConstruct
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 *
 */
@Slf4j("logger")
@Controller
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

//    @Autowired
//    Environment env

    @Autowired
    MeterRegistry registry

    DataCache cache
    File workingDir
    List<SearchDomain> cores = [
            new SearchDomain('cord_2020_06_12', 'CORD-19', true),
            new SearchDomain('pubmed', 'PubMed (coming soon)', false),
            new SearchDomain('pmc', 'PubMed Central (coming soon)', false)
    ]
    Counter questionsAsked

    public AskController() {
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
//        registry.config().commonTags("application", "askme")
        questionsAsked = registry.counter("questions")
    }

    @GetMapping(path="/show", produces = ['text/html'])
    @ResponseBody String getShow(@RequestParam String path) {
        String body = "<body><h1>Error</h1><p>An error occured retrieving $path</p></body>"
        String xml = fetch(path)
        if (xml) {
            body = transform(xml)
        }
        return """
<html>
    <head>
        <title>$path</title>
        <style>
            body {
                font-size: 12pt;
                margin: 10em;
            }
        </style>
    </head>
    $body
<html>
"""
    }

    @Timed(value = "get_ask", percentiles = [0.5d, 0.95d])
    @GetMapping(path = "/ask", produces = ['text/html'])
    String getAsk(Model model) {
        logger.info("GET /ask")
        updateModel(model)
        List<String> descriptions = [
                ["consecutive terms", "Runs of consecutive search terms score higher."],
                ["total search terms", "More search terms in the section means a higher score."],
                ["position", "Search terms that appear early in the section score higher."],
                ["% search terms", "The percentage of terms that appear in the section."],
                ["term order", "If searching for 'X Y' then '..X...Y.' scores higher than '..Y...X.'"],
                ["1st sentence", "Search terms in the first sentence score higher."],
                ["sentence count", "Weighted by the # of sentences that contain at least one search term."]
        ]
        model.addAttribute("descriptions", descriptions)
        model.addAttribute("cores", cores)
        logger.debug("Rendering mainpage")
        return "mainpage"
    }

    @GetMapping(path = '/test', produces = "text/plain")
    ResponseEntity<String> getTest() {
        return ResponseEntity.ok("test")
//        return 'test'
    }

    @PostMapping(path="/test", produces = "text/html")
    String postTest(@RequestParam(defaultValue = 'undefined') String username, @RequestParam(defaultValue = 'undefined') String dataset, Model model) {
        model.addAttribute('username', username)
        model.addAttribute('dataset', dataset)
        return 'test'
    }

    @GetMapping(path="/validate")
    @ResponseBody String getValidate(@RequestParam String email) {
        String url = GALAXY_HOST + '/api/users?key=' + config.GALAXY_KEY + '&f_email=' + email
//        String json = new URL(url).text
        logger.debug("Validating email {}", email)
        Map status = [ valid: false]
        try {
            JsonSlurper parser = new JsonSlurper()
            List users = parser.parse(new URL(url))
            if (users.size() == 1 && users[0].email == email) {
                logger.trace("Valid email {}", email)
                status.valid = true
            }
        }
        catch (Exception e) {
            logger.warn("Unable to validate {}: {}", email, e)
        }
        String json = Serializer.toJson(status)
        logger.debug("Returning: {}", json)
        return json
    }

    @GetMapping(path='/rate')
    @ResponseBody String getRate(@RequestParam String key, @RequestParam String score) {
        int value = score as int
        db.rate(key, value)
//        ratings.save(new Rating(key, value))
        String result = 'Unknown'
        switch (value) {
            case -1:
                result = "Bad"
                break
            case 0:
                result = "Meh"
                break
            case 1:
                result = "Good"
                break
        }
        return result
    }

    @GetMapping(path='/ratings', produces='text/html')
    String getRatings(Model model) {
        updateModel(model)
        model.addAttribute('data', db.ratings())
        return 'ratings'
    }

    @GetMapping(path='/questions', produces = 'text/html')
    String getQuestions(Model model) {
        updateModel(model)
        model.addAttribute('data', db.questions())
        return 'questions'
    }

    @PostMapping(path="/save", produces="text/html")
    String postSave(@RequestParam String key, @RequestParam String username, Model model) {
//    String saveDocuments(@RequestParam Map<String,String> params, Model model) {
        logger.debug("Sending documents to Galaxy.")
        updateModel(model)

        String json = cache.get(key)
        if (json == null) {
            logger.warn("Data for {} was not found in the cache.", key)
            model.addAttribute('message', 'The data was not found in the cache!')
            return 'error'
        }

//        JsonSlurper parser = new JsonSlurper()
//        Map data = parser.parseText(json)
        Packet packet = Serializer.parse(json, Packet)

        File zipFile = new File(workingDir, "${key}.zip")
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(zipFile))

        // Create the zip file.
        packet.documents.each { Document doc ->
//            String id = getId(doc)
            if (doc.id) try {
                Container container = new Container()
                container.text = doc.body?.text ?: doc.articleAbstract.text
                container.language = 'en'
                container.metadata.pmid = doc.pmid
                container.metadata.title = doc.title
                container.metadata.year = doc.year

                String zipPath = "$username/${doc.id}.lif"
                ZipEntry entry = new ZipEntry(zipPath)
                zip.putNextEntry(entry)
                zip.write(payload(container))
                zip.closeEntry()
            }
            catch (Exception e) {
                logger.error("Unable to zip document {}", doc.id, e)
            }
        }
        zip.close()

        // Send the zip file to the upload service.
        PostOffice po
        long nBytes = 0
        try {
            // Send the zip to the upload service.
            po = new PostOffice(UPLOAD_POSTOFFICE, config.HOST)
            po.send(UPLOAD_ADDRESS, zipFile.bytes)
            po.close()
            nBytes = zipFile.bytes.length
            logger.info("Posted {} bytes to Galaxy.", nBytes)
        }
        catch (Exception e) {
            logger.error("Unable to post files to galaxy.", e)
            model.addAttribute('error_message', e.getMessage())
        }
        finally {
            if (po != null) {
                po.close()
            }
        }
        model.addAttribute('size', packet.documents.size())
        model.addAttribute('path', zipFile.path)
        model.addAttribute('bytes', nBytes)
        if (!zipFile.delete()) {
            logger.error("Unable to delete {}", zipFile.path)
            zipFile.deleteOnExit()
        }
        return 'saved'
    }

    String getId(Map doc) {
        if (doc.pmid) return doc.pmid
        if (doc.pmc) return doc.pmc
        if (doc.doi) return doc.doi
        if (doc.id) return doc.id
        return null
    }

    byte[] payload(Container container) {
        Data data = new Data(Discriminators.Uri.LIF, container)
        return data.asJson().bytes
    }

    @Timed(value = "post_question", percentiles = [0.5d, 0.95d])
    @PostMapping(path="/question", produces="text/html")
    String postQuestion(@RequestParam Map<String,String> params, Model model) {
        logger.info("POST /question")
        logger.info(params.question)
        questionsAsked.increment()
        updateModel(model)
//        if (true) return "ask"

        String uuid = UUID.randomUUID()
        saveQuestion(uuid, params)

        long start = System.currentTimeMillis()
        Packet reply = answer(params, 100)
        new File("/tmp/packet.json").text = Serializer.toPrettyJson(reply)
//        println Serializer.toPrettyJson(reply)
        long duration = System.currentTimeMillis() - start
//        model.addAttribute("duration", duration)

        Map data = [:]
        data.documents = reply.documents
        data.query = reply.query
        if (reply.documents.size() > 0) {
            Document exemplar = reply.documents[0]
            data.keys = exemplar.scores.keySet()
        }

        if (reply.status == Status.TIMEOUT || reply.status == Status.ERROR) {
            if (reply.message) {
                model.addAttribute('error', reply.message)
            }
            return 'error'
        }

        data.documents.each { Document doc ->
            if (doc.url.contains("; ")) {
                doc.url = getBestUrl(doc.url)
            }
        }
        cache.add(uuid, reply)
        model.addAttribute('data', data)
        model.addAttribute('key', uuid)
        model.addAttribute('duration', Utils.format(duration))
        logger.debug("Rendering data")
        //return Serializer.toPrettyJson(reply)
        return 'answer'
    }

    private String getBestUrl(String url) {
        // There is only one, return it.
        if (!url.contains(';')) {
            return url
        }
        List<String> candidates = url.tokenize(";").collect {it.trim() }
        // Search for the best candidate in order
        ['sciencedirect', 'doi.org', 'ncbi'].each {
            String best = from(candidates, it)
            if (best) return best
        }
        // Nothing looks "best" so return the first.
        return candidates[0]
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

    private void saveQuestion(String uuid, Map<String,String> data) {
        Thread.start {
            String question = data.question
            db.save(new Question(uuid, question))
            data.each { k,v ->
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
        packet.query = new Query(params.question, 1000)
        message.setBody(packet)
        message.setRoute([config.QUERY_MBOX, config.SOLR_MBOX, config.RANKING_MBOX, config.WEB_MBOX])
        message.setParameters(params)
        logger.trace('Sending the message')
        PostalService.Delivery delivery = po.send(message)
        logger.trace("Waiting for a response")
//        synchronized (lock) {
//            lock.wait(120000)
//        }
        message = delivery.get(60, TimeUnit.SECONDS) as AskmeMessage
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

        if (result.documents.size() > size) {
            logger.debug("Trimming results to {}", size)
            result.documents = result.documents[0..size]
        }
        return result
    }

    private String transform(String xml) {
        XmlParser parser = Factory.createXmlParser()

        def article = parser.parseText(xml)

//        transformations.sec = { div([:], null) }
//        transformations.italic = { em([:], null) }
//        transformations.xref = { strong([:], null) }
//        transformations.title = { title([:], null) }
        Node body = article.body[0]
        if (body == null) {
            return xml
        }

        List<Node> dfs = body.depthFirst()
        replaceAll(dfs, 'sec', 'div')
        replaceAll(dfs, 'italic', 'em')
        replaceAll(dfs, 'xref', 'strong')
        replaceAll(dfs, 'title', 'h1')

//        Node html = new Node(null, 'html')
//        Node head = new Node(html, 'head')
//        new Node(head, 'title', 'PMC')
//        html.append(body)

        StringWriter writer = new StringWriter()
        XmlNodePrinter printer = new XmlNodePrinter(new PrintWriter(writer))
        printer.print(body)
        return writer.toString()
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

    String fetch(String path) {
        Object lock = new Object()
        String returnAddress = UUID.randomUUID().toString()
        PostOffice po = new PostOffice(config.EXCHANGE, config.HOST)
        String xml = null //'<body><h1>Error</h1><p>There was a problem loading the document content.</p></body>'
        MailBox box = new MailBox(config.EXCHANGE, returnAddress, config.HOST) {
            void recv(String json) {
                try {
                    Message message = Serializer.parse(json, Message)
                    xml = message.body
                    if (message.command == 'loaded') {
                        xml = message.body
                    }
                    else {
                        println "ERROR: ${message.command}"
                        println "BODY: " + message.body
                    }
                }
                catch (Exception e) {
                    e.printStackTrace()
                    throw e
                }
                finally {
                    synchronized (lock) {
                        lock.notifyAll()
                    }
                }
            }
        }

        Message message = new Message()
                .command('load')
                .body(path)
                .route('load')
                .route(returnAddress)

        po.send(message)
        synchronized (lock) {
            lock.wait(4000)
        }
        po.close()
        box.close()
        return xml
    }

    /*
    Map geodeepdive(Map params, int size) {
        println "GeoDeepDive limit: $size"
        geoProcessor.limit = size
        Query query = geoProcessor.transform(params.question)
        println "Query: ${query.query}"
        println "Terms: ${query.terms.join", "}"
        String json = new URL(query.query).text
        Map data = new JsonSlurper().parseText(json)
        if (data.size() == 0) {
            return null
        }

        List<Document> docs = []
        data.success.data.each { record ->
            GDDDocument doc = new GDDDocument()
            doc.title = record.title
            doc.highlight = record.highlight
            doc.hits = record.hits
            doc.doi = record.doi
            doc.year = record.coverDate
            docs.add(doc)
        }

        Map result = [:]
        result.query = query
        result.size = docs.size()

        List<Document> ranked = rank(query, docs, params, { it.highlight })
        if (ranked.size() > size) {
            result.documents = ranked[0..size]
        }
        else {
            result.documents = ranked
        }
        if (result.documents.size() > 0) {
            Document exemplar = result.documents[0]
            result.keys = exemplar.scores?.keySet()
        }
        return result

    }
    */

    private void updateModel(Model model) {
        model.addAttribute('version', Version.version)
        if (messages.hasMessage()) {
            model.addAttribute('message', messages.message)
        }
    }

//    @ExceptionHandler(Exception.class)
//    protected String handleAddExceptions(Exception ex, WebRequest request) {
//        logger.error("Caught an exception", ex)
//        return "error"
//    }
}
