package org.lappsgrid.askme.web.services

import groovy.util.logging.Slf4j
import org.lappsgrid.askme.core.Configuration
import org.lappsgrid.askme.core.concurrent.*
import org.lappsgrid.rabbitmq.Message
import org.lappsgrid.rabbitmq.topic.MailBox
import org.lappsgrid.rabbitmq.topic.MessageBox

/**
 *
 */
@Slf4j
class POBox extends MessageBox {

    Configuration config = new Configuration()
    PostalService po

    POBox(PostalService po) {
        super(config.EXCHANGE, config.WEB_MBOX, config.HOST)
        this.po =  po
    }

    @Override
    void recv(Message message) {
        PostalService.Delivery delivery = po.get(message.id)
        if (delivery == null) {
            log.warn("Received message {} with no delivery registered.", message.id)
            return
        }
//        storage.put(message.id, message)
        po.deliver(message)
//        delivery.send()
    }
}
