package org.lappsgrid.askme.web.services

import groovy.util.logging.Slf4j
import org.lappsgrid.askme.core.Configuration
import org.lappsgrid.askme.core.concurrent.Signal
import org.lappsgrid.rabbitmq.topic.MailBox

/**
 *
 */
@Slf4j
class POBox extends MailBox {

    Configuration config = new Configuration()
    PostalService po

    POBox(PostalService po) {
        super(config.EXCHANGE, config.WEB_MBOX, config.HOST)
        this.po =  po
    }

    @Override
    void recv(String message) {
        Signal signal = po.getSignal(message.id)
        if (signal == null) {
            log.warn("Received message {} with no signal registered.", message.id)
            return
        }
//        storage.put(message.id, message)
        po.store(message)
        signal.send()
    }
}
