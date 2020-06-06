package org.lappsgrid.askme.web

import org.lappsgrid.askme.core.ssl.SSL
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

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
}
