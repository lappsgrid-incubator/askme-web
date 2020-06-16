package org.lappsgrid.askme.web.services

import groovy.util.logging.Slf4j
import org.lappsgrid.askme.core.Configuration
import org.lappsgrid.askme.core.api.AskmeMessage
import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.topic.MailBox
import org.lappsgrid.rabbitmq.topic.MessageBox
import org.lappsgrid.serialization.Serializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Handles incoming messages and delivers them to the PostalService
 */
@Slf4j
@Service
class POBox {
    static final Configuration config = new Configuration()
    PostalService po
    Box web
    Box metrics

    @Autowired
    POBox(PostalService po) {
        log.info("Creating POBox")
        this.po =  po
        this.web = new Box(po, AskmeMessage, config.WEB_MBOX)
        this.metrics = new Box(po, Message, config.METRICS_MBOX)
        log.info("Created POBox")
    }

    class Box extends MailBox {
        PostalService po
        Class parsedClass

        Box(PostalService po, Class parsedClass, String address) {
            super(config.EXCHANGE, address, config.HOST)
            this.po = po
            this.parsedClass = parsedClass
        }
        @Override
        void recv(String message) {
//            PostalService.Delivery delivery = po.get(message.id)
//            if (delivery == null) {
//                log.warn("Received message {} with no delivery registered.", message.id)
//                return
//            }
            po.deliver(Serializer.parse(message, parsedClass))
        }
    }
}
