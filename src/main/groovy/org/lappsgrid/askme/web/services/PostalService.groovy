package org.lappsgrid.askme.web.services

import groovy.util.logging.Slf4j
import org.lappsgrid.askme.core.Configuration
import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.topic.MailBox
import org.lappsgrid.rabbitmq.topic.MessageBox
import org.lappsgrid.rabbitmq.topic.PostOffice
import org.lappsgrid.askme.core.Signal
import org.springframework.stereotype.Service

import java.util.concurrent.ConcurrentHashMap

/**
 *
 */
@Slf4j
@Service
class PostalService {

    Configuration config
    PostOffice po
    POBox box
    Map<String, String> storage
    Map<String, Signal> signals

    PostalService() {
        config = new Configuration()
        po = new PostOffice(config.EXCHANGE, config.HOST)
        box = new POBox()
        storage = new ConcurrentHashMap<>()
        signals = new ConcurrentHashMap<>()
    }

    void close() {
        signals.each { name, sig -> sig.send() }
        po.close()
        box.close()
        signals.clear()
        storage.clear()
    }

    Signal send(Message message) {
        Signal signal = new Signal()
        signals.put(message.id, signal)
        po.send(message)
        return signal
    }

    String pickup(String id) {
        return storage.remove(id)
    }

    class POBox extends MailBox {

        POBox(String exchange, String address) {
            super(config.EXCHANGE, config.WEB_MBOX, config.HOST)
        }

        @Override
        void recv(String message) {
            Signal signal = signals.remove(message.id)
            if (signal == null) {
                log.warn("Received message {} with no signal registered.", message.id)
                return
            }
            storage.put(message.id, message)
            signal.send()
        }
    }
}
