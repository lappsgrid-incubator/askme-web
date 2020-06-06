package org.lappsgrid.askme.web.services

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 *
 */
@Slf4j
//@Service
class MailBoxService {

    PostalService po
    POBox box

    @Autowired
    MailBoxService(PostalService po) {
        log.info("Starting")
        this.po = po
        log.info("Started")
    }

    @PostConstruct
    private void init() {
        log.info("Initializing")
//        box = new POBox(po)
        log.info("Initialized")
    }
    @PreDestroy
    void close() {
        log.info("Closing")
        box.close()
        log.info("Closed")
    }
}
