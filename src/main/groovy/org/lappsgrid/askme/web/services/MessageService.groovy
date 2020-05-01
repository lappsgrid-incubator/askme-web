package org.lappsgrid.askme.web.services

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Service

/**
 *
 */
@Slf4j("logger")
@Service
class MessageService {

    private static final String[] DEFAULT_MESSAGE = [
            'The AskMe service is under constant development and may go down for maintenance without warning.',
            'We apologize for any inconvenience this may cause.'

    ] as String[]

    private String[] message = DEFAULT_MESSAGE

    boolean hasMessage() { return message != null }
    void setMessage(final String message = null) {
        if (message != null) {
            logger.debug("Setting message.")
            this.message = message.split('\n')
        }
        else if (message == 'none') {
            this.message = null;
        }
        else {
            logger.debug("Reverting message to default.")
            this.message = DEFAULT_MESSAGE
        }
    }

    String[] getMessage() {
        return message
    }
}
