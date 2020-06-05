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
            '<h2>May 3, 2020 12:00 UTC</h2>',
            '<p>The index has been updated to include access to 68,850 documents from the <a href="https://www.semanticscholar.org/cord19/download">May 1 CORD-19 dataset</a>.</p>',
            '<p>The AskMe service is under constant development and may go down for maintenance without warning. We apologize for any inconvenience this may cause.</p>'
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
