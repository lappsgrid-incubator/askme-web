package org.lappsgrid.askme.web.controllers

import org.lappsgrid.askme.core.Configuration
import org.lappsgrid.askme.core.Signal
import org.lappsgrid.askme.web.NotFoundError
import org.lappsgrid.askme.web.services.PostalService
import org.lappsgrid.rabbitmq.Message
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import java.util.concurrent.TimeUnit

/**
 *«
 */
@RestController
@RequestMapping("/metrics")
class MetricController {

    Configuration config = new Configuration()

    PostalService po

    @Autowired
    MetricController(PostalService po) {
        this.po = po
    }

    @GetMapping(path = "/ranking", produces = MediaType.TEXT_PLAIN_VALUE)
    String getRankingMetrics() {
        Message message = new Message()
        message.command = "metrics"
        message.route(config.RANKING_MBOX, config.WEB_MBOX)

        Signal signal = po.send(message)
        if (!signal.await(1, TimeUnit.SECONDS)) {
            throw NotFoundError("The ranking service is not responding")
        }
    }
}
