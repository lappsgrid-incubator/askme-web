package org.lappsgrid.askme.web.controllers

import io.micrometer.core.annotation.Timed
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
 * Return metrics for the other services to be consumed by Grafana.
 * <p>Since the other services do not expose any public ports the web controller
 * acts as their proxy.
 * </p>
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

    @Timed(value = "get_ranking_metrics", percentiles = [0.5d, 0.95d])
    @GetMapping(path = "/ranking", produces = MediaType.TEXT_PLAIN_VALUE)
    String getRankingMetrics() {
        return getMetrics(config.RANKING_MBOX)
    }

    @Timed(value = "get_solr_metrics", percentiles = [0.5d, 0.95d])
    @GetMapping(path = "/solr", produces = MediaType.TEXT_PLAIN_VALUE)
    String getSolrMetrics() {
        return getMetrics(config.SOLR_MBOX)
    }

    @Timed(value = "get_query_metrics", percentiles = [0.5d, 0.95d])
    @GetMapping(path = "/query", produces = MediaType.TEXT_PLAIN_VALUE)
    String getQueryMetrics() {
        return getMetrics(config.QUERY_MBOX)
    }

    String getMetrics(String service) {
        Message message = new Message()
        message.command = "METRICS"
        message.route(service, config.METRICS_MBOX)

        PostalService.Delivery delivery = po.send(message)
        Object object = delivery.get(3, TimeUnit.SECONDS)
        if (object == null) {
            throw new NotFoundError("The $service service is not responding")
        }
        message = object as Message
        if ("ok" != message.command) {
            throw new InternalServerError(message.body ?: "There was a problem getting the metrics from the $service service")
        }
        return message.body
    }
}
