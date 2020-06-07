package org.lappsgrid.askme.web.controllers

import org.lappsgrid.askme.core.Configuration
import org.lappsgrid.askme.core.concurrent.Signal
import org.lappsgrid.askme.web.errors.InternalServerError
import org.lappsgrid.askme.web.errors.NotFoundError
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
        return getMetrics(config.RANKING_MBOX)
    }

    @GetMapping(path = "/solr", produces = MediaType.TEXT_PLAIN_VALUE)
    String getSolrMetrics() {
        return getMetrics(config.SOLR_MBOX)
    }

    @GetMapping(path = "/query", produces = MediaType.TEXT_PLAIN_VALUE)
    String getQueryMetrics() {
        return getMetrics(config.QUERY_MBOX)
    }

    String getMetrics(String service) {
        Message message = new Message()
        message.command = "metrics"
        message.route(service, config.WEB_MBOX)

        Signal signal = po.send(message)
        if (!signal.await(1, TimeUnit.SECONDS)) {
            throw new NotFoundError("The $service service is not responding")
        }
        message = po.pickup(message.id)
        if ("ok" != message.command) {
            throw new InternalServerError(message.body ?: "There was a problem getting the metrics from the ranking service")
        }
        return message.body()
    }
}
