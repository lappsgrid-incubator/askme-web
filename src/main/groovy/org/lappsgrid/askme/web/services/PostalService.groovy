package org.lappsgrid.askme.web.services

import groovy.util.logging.Slf4j
import org.lappsgrid.askme.core.Configuration
import org.lappsgrid.askme.core.concurrent.Signal
import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.topic.PostOffice
import org.springframework.stereotype.Service

import javax.annotation.PreDestroy
import java.util.concurrent.ConcurrentHashMap

/**
 * Maintains a single connection to the RabbitMQ server.
 */
@Slf4j
@Service
class PostalService {

    static final Configuration config = new Configuration()
    PostOffice po
    POBox box

    Map<String, String> storage
    Map<String, Signal> signals

    PostalService() {
        log.info("PostalService connecting to host {} exchange {}", config.HOST, config.EXCHANGE)
        po = new PostOffice(config.EXCHANGE, config.HOST)
        storage = new ConcurrentHashMap<>()
        signals = new ConcurrentHashMap<>()
        log.info("PostalService started.")
    }

    @PreDestroy
    void close() {
        log.info("Closing PostalService")
        signals.each { name, sig -> sig.send() }
        po.close()
        if (box) box.close()
        signals.clear()
        storage.clear()
        log.info("PostalService closed")
    }

    Signal send(Message message) {
        if (box == null) {
            box = new POBox(this)
        }
        Signal signal = new Signal()
        signals.put(message.id, signal)
        po.send(message)
        return signal
    }

    Signal getSignal(String id) {
        return signals.remove(id)
    }

    void store(Message message) {
        storage[message.id] = message
    }

    String pickup(String id) {
        return storage.remove(id)
    }

}
