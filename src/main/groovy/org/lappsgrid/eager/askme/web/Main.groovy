package org.lappsgrid.eager.askme.web

import org.lappsgrid.eager.mining.api.Query
import org.lappsgrid.eager.query.SimpleQueryProcessor

/**
 *
 */
class Main {
    
    void run() {
        SimpleQueryProcessor processor = new SimpleQueryProcessor()
        Query query = processor.transform("What proteins bind to the PDGF-alpha receptor in neural stem cells?")
        println query.query
    }
    
    static void main(String[] args) {
        new Main().run()
    }
}
