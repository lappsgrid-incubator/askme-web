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
            '<h2>June 16, 2020 12:09 UTC</h2>',
            '<p>The index has been updated to include 64,668 documents from the <a href="https://ai2-semanticscholar-cord-19.s3-us-west-2.amazonaws.com/historical_releases.html">June 12 dataset</a>.</p>',
            '<p><strong>Coming Soon</strong> Daily updates to the index.</p>'
//            '<h2>June 15, 2020 12:00 UTC</h2>',
//            'The Solr index is being updated and queries may return inconsistent, or even empty results. This message will be updated when the update has been completed.'
//            '<h2>June 15, 2020 12:00 UTC</h2>',
//            '<p>The index has been updated to include access to the <a href="https://www.semanticscholar.org/cord19/download">June 12 CORD-19 dataset</a>.</p>',
//            '<p>The AskMe service is under constant development and may go down for maintenance without warning. We apologize for any inconvenience this may cause.</p>'
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
