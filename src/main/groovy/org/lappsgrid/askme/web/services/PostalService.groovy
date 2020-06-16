package org.lappsgrid.askme.web.services

import groovy.util.logging.Slf4j
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.lappsgrid.askme.core.Configuration
import org.lappsgrid.askme.core.concurrent.Future
import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.topic.PostOffice
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import java.util.concurrent.ConcurrentHashMap

/**
 * Maintains a single connection to the RabbitMQ server.
 */
@Slf4j
@Service
class PostalService {

    static final Configuration config = new Configuration()

    @Autowired
    MeterRegistry registry

    PostOffice po
//    Gauge outstandingDeliveries

//    POBox box

//    Map<String, String> storage
//    Map<String, Signal> signals
    Map<String,Delivery> deliveries

    PostalService() {
        log.info("PostalService connecting to host {} exchange {}", config.HOST, config.EXCHANGE)
        po = new PostOffice(config.EXCHANGE, config.HOST)
//        deliveries = new ConcurrentHashMap<>()
//        signals = new ConcurrentHashMap<>()
        log.info("PostalService started.")
//        outstandingDeliveries = registry.gaugeMapSize("deliveries")
    }

    @PostConstruct
    void init() {
        deliveries = registry.gaugeMapSize("deliveries.outstanding", Tags.empty(), new ConcurrentHashMap<>())
    }

    @PreDestroy
    void close() {
        log.info("Closing PostalService")
//        signals.each { name, sig -> sig.send() }
        po.close()
//        if (box) box.close()
        deliveries.clear()
//        storage.clear()
        log.info("PostalService closed")
    }

    Delivery send(Message message) {
//        if (box == null) {
//            box = new POBox(this)
//        }
//        Signal signal = new Signal()
//        signals.put(message.id, signal)
//        po.send(message)
//        return signal
//        if (message.route.size() == 1 || config.WEB_MBOX != message.route[0]) {
//            message.route(config.WEB_MBOX)
//        }
        log.debug("Recording delivery for {}", message.id)
        Delivery delivery = new Delivery()
        deliveries[message.id] = delivery
        Delivery d = deliveries.get(message.id)
        if (d == null) {
            log.error("Message {} has NOT been recorded", message.id)
        }
        else {
            log.debug("There are {} outstanding deliveries", deliveries.size())
        }
        po.send(message)
        log.trace("Message sent")
        return delivery
    }

//    Delivery remove(String id) {
//        log.debug("Removing delivery for {}", id)
//        return deliveries.remove(id)
//    }
//
//    Delivery get(String id) {
//        log.debug("Getting delivery for {}", id)
//        return deliveries.get(id)
//    }

    void deliver(Message message) {
        log.debug("Message {} has been delivered", message.id)
        Delivery delivery = deliveries.remove(message.id)
        if (delivery == null) {
            log.warn("Unexpected delivery of message {}", message.id)
            log.debug("There are {} outstanding deliveries", deliveries.size())
            return
        }
        log.trace("Sending delivery notification")
        delivery.set(message)
    }

//    String pickup(String id) {
//        return storage.remove(id)
//    }

    class Delivery extends Future { }
}
