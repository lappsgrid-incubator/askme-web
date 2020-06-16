package org.lappsgrid.askme.web

import io.micrometer.core.instrument.MeterRegistry
import org.lappsgrid.askme.core.ssl.SSL
import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

/**
 *
 */
@SpringBootApplication
class EntryPoint {
    static void main(String[] args) {
//        System.setProperty("spring.config.location", "classpath:applicatiion.properties,classpath:askme.yml")
        SSL.enable()
        SpringApplication.run(EntryPoint, args)
    }

//    @Bean
//    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
//        return { registry -> registry.config().commonTags("application", "askme"); }
//    }
}
