package org.lappsgrid.askme.web.services

import groovy.util.logging.Slf4j
import org.lappsgrid.askme.core.Configuration
import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.topic.MailBox
import org.lappsgrid.rabbitmq.topic.MessageBox
import org.lappsgrid.rabbitmq.topic.PostOffice
import org.lappsgrid.askme.core.concurrent.*
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

//    Map<String, String> storage
//    Map<String, Signal> signals
    Map<String,Delivery> deliveries

    PostalService() {
        log.info("PostalService connecting to host {} exchange {}", config.HOST, config.EXCHANGE)
        po = new PostOffice(config.EXCHANGE, config.HOST)
        deliveries = new ConcurrentHashMap<>()
//        signals = new ConcurrentHashMap<>()
        log.info("PostalService started.")
    }

    @PreDestroy
    void close() {
        log.info("Closing PostalService")
//        signals.each { name, sig -> sig.send() }
        po.close()
        if (box) box.close()
        deliveries.clear()
//        storage.clear()
        log.info("PostalService closed")
    }

    Delivery send(Message message) {
        if (box == null) {
            box = new POBox(this)
        }
//        Signal signal = new Signal()
//        signals.put(message.id, signal)
//        po.send(message)
//        return signal
        Delivery delivery = new Delivery()
        deliveries[message.id] = delivery
        po.send(message)
        return delivery
    }

    Delivery get(String id) {
        return deliveries.remove(id)
    }

    void deliver(Message message) {
        Delivery delivery = deliveries.get(message.id)
        if (delivery == null) {
            log.warn("Unexpected delivery of message {}", message.id)
            return
        }
        delivery.set(message)
    }

//    String pickup(String id) {
//        return storage.remove(id)
//    }

    class Delivery extends Future { }
}
