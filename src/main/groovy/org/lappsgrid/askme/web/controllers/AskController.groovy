package org.lappsgrid.askme.web.controllers

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.lappsgrid.askme.core.Configuration
import org.lappsgrid.askme.core.ssl.SSL
import org.lappsgrid.askme.web.Version
import org.lappsgrid.askme.web.db.Database
import org.lappsgrid.askme.web.db.Question
import org.lappsgrid.askme.web.util.DataCache
import org.lappsgrid.discriminator.Discriminators
import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.topic.MailBox
import org.lappsgrid.rabbitmq.topic.MessageBox
import org.lappsgrid.rabbitmq.topic.PostOffice
import org.lappsgrid.serialization.Data

import org.lappsgrid.serialization.Serializer
import org.lappsgrid.serialization.lif.Container
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.WebRequest

import javax.annotation.PostConstruct
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

//    @Autowired
//    private final AskmeSettings settings

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
    Database db

    @Autowired
    Environment env

    // TODO The DocumentProcessor fetches documents from solr.
//    DocumentProcessor documentProcessor
    DataCache cache
    File workingDir

    PostOffice po

    public AskController() {
//        queryProcessor = new SimpleQueryProcessor()
//        geoProcessor = new GDDSnippetQueryProcessor()
//        documentProcessor = new DocumentProcessor()
//        settings = new AskmeSettings()
        SSL.enable()
        po = new PostOffice(config.EXCHANGE, config.HOST)
    }

    @PostConstruct
    private void init() {
        cache = new DataCache(CACHE_DIR, CACHE_TTL as Integer)
        workingDir = new File(WORK_DIR)
        if (!workingDir.exists()) {
            workingDir.mkdirs()
        }

        /*
        config = new ConfigObject()
        Map m = [:]
        set(m, 'solr.host')
        set(m, 'solr.collection')
        set(m, 'solr.rows')
        set(m, 'galaxy.host')
        set(m, 'galaxy.key', System.getenv('GALAXY_API_KEY'))
        set(m, 'root')
        set(m, 'work.dir')
        set(m, 'question.dir')
        set(m, 'cache.dir')
        set(m, 'cache.ttl')
        set(m, 'upload.postoffice')
        set(m, 'upload.address')
        config.putAll(m)
        if (config.cache.ttl) {
            cache = new DataCache(config.cache.dir, Integer.parseInt(config.cache.ttl.trim()))
        }
        else {
            logger.warn("Cache TTL was not found in the configuration")
            cache = new DataCache(config.cache.dir)
        }

        workingDir = new File(config.work.dir)
        if (!workingDir.exists()) {
            workingDir.mkdirs()
        }
        */
    }

    /*
    void set(Map map, String key) {
        String value = env.getProperty(key)
        set(map, key, value)
    }

    void set(Map map, String key, String value) {
        set(map, key.tokenize('.'), value)
    }

    void set(Map map, List parts, String value) {
        if (parts.size() == 1) {
            map[parts[0]] = value
        }
        else {
            String key = parts.remove(0)
            Map current = map[key]
            if (current == null) {
                current = [:]
                map[key] = current
            }
            set(current, parts, value)
        }
    }
    */

//    @GetMapping(path="/env", produces = ['text/plain'])
//    String printEnv(Model model) {
//        model.addAttribute("solr_host", settings.solr.host)
//        model.addAttribute("solr_collection", settings.solr.collection)
//        return "environment"
//    }

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

    /*
                        td 'Number of consecutive terms in title'
                        td 'Total number of search terms in title'
                        td 'Term position in title, earlier in the text == better score'
                        td 'Words in the title that are search terms'
     */
    @GetMapping(path = "/ask", produces = ['text/html'])
    String getAsk(Model model) {
        logger.info("GET /ask")
        updateModel(model)
        List<String> descriptions = [
                "consecutive terms",
                "total search terms",
                "position",
                "% search terms",
                "term order",
                "1st sentence",
                "sentence count"
        ]
        model.addAttribute("descriptions", descriptions)
        logger.debug("Rendering mainpage")
        return "mainpage"
    }

    @GetMapping(path = '/test', produces = "text/html")
    String getTest() {
        return 'test'
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

        JsonSlurper parser = new JsonSlurper()
        Map data = parser.parseText(json)

        File zipFile = new File(workingDir, "${key}.zip")
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(zipFile))

        // Create the zip file.
        data.documents.each { doc ->
            String id = getId(doc)
            if (id) try {
                Container container = new Container()
                container.text = doc.body
                container.language = 'en'
                container.metadata.pmid = doc.pmid
                container.metadata.title = doc.title
                container.metadata.year = doc.year

                String zipPath = "$username/${id}.lif"
                ZipEntry entry = new ZipEntry(zipPath)
                zip.putNextEntry(entry)
                zip.write(payload(container))
                zip.closeEntry()
            }
            catch (Exception e) {
                logger.error("Unable to zip document {}", id, e)
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
        model.addAttribute('size', data.documents.size())
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

    //TODO Remove @ResponseBody and change produces to text/html
    @PostMapping(path="/question", produces="application/json")
    @ResponseBody String postQuestion(@RequestParam Map<String,String> params, Model model) {
        logger.info("POST /question")
        updateModel(model)
        String uuid = UUID.randomUUID()
        //TODO Save the questions again.
//        saveQuestion(uuid, params)

        long start = System.currentTimeMillis()
        Map reply = answer(params, 100)
        long duration = System.currentTimeMillis() - start
        reply.duration = duration

        //TODO re-enable the HTML page.
        /*
        cache.add(uuid, reply)
        model.addAttribute('data', reply)
        model.addAttribute('key', uuid)
        model.addAttribute('duration', Utils.format(duration))
        */
        logger.debug("Rendering data")
        return Serializer.toPrettyJson(reply)
        //return 'answer'
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

    private Map answer(Map params, int size) {
        logger.debug("Generating answer.")

        Object lock = new Object()
        Map result = [:]
        boolean timeout = true

        logger.trace('Constructing the message.')
        Message message = new Message()
        message.setBody(params.question)
        message.setRoute([config.QUERY_MBOX, config.SOLR_MBOX, config.RANKING_MBOX, message.getId()])
        message.setParameters(params)

        MessageBox box = new MessageBox(config.EXCHANGE, message.getId(), config.HOST) {

            @Override
            void recv(Message m) {
                //println "Received reply from query service."

                result.message = m
                result.documents = m.body
                timeout = false
                synchronized (lock) {
                    lock.notifyAll()
                }
            }
        }

        logger.trace('Sending the message')
        po.send(message)
        logger.trace("Waiting for a response")
        synchronized (lock) {
            lock.wait(30000)
        }
        if (timeout) {
            logger.warn("Operation timed out")
            result.error = "Operation timed out."
        }

        logger.trace('Shutting down MessageBox')
        box.close()


        result.query = params.query
        result.size = 0

        // TODO Determine how much of this we still need.
        List docs = [] //documentProcessor.process(documents)
//        List docs = []
//        for (int i = 0; i < n; ++i) {
//            SolrDocument doc = documents.get(i)
//             docs << new Document(doc)
//        }

        /*
        // TODO We need a session managed bean so multiple users do not overwrite each other's files.
        File base = new File("/tmp/eager")
        if (!base.exists()) {
            base.mkdirs()
        }
        new File(base, 'query.json').text = Serializer.toPrettyJson(query)
        new File(base, 'files.json').text = Serializer.toPrettyJson(docs)
        new File(base, 'params.json').text = Serializer.toPrettyJson(params)

        result.documents = [] //rank(query, docs, params)
        if (result.documents.size() > size) {
            logger.debug("Trimming results to {}", size)
            result.documents = result.documents[0..size]
        }
//        if (result.documents.size() > 0) {
//            Document exemplar = result.documents[0]
//            result.keys = exemplar.scores.keySet()
//        }
*/
        return result
    }

    /*
    private List rank(Query query, List<Document> documents, Map params) {
        logger.debug("Ranking {} documents", documents.size())
        RankingProcessor process = new RankingProcessor(params)
        return process.rank(query, documents)
    }

    private List rank(Query query, List<Document> documents, Map params, Closure getter) {
        logger.debug("Ranking {} documents", documents.size())
        RankingEngine ranker = new RankingEngine(params)
        return ranker.rank(query, documents, getter)
    }
    */

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
    }

    @ExceptionHandler(Exception.class)
    protected String handleAddExceptions(Exception ex, WebRequest request) {
        logger.error("Caught an exception", ex)
    }
}
