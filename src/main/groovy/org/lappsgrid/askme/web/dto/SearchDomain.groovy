package org.lappsgrid.askme.web.dto

/**
 *
 */
class SearchDomain {
    String id
    String label
    boolean enabled

    SearchDomain() {}
    SearchDomain(String id, String label, boolean enabled = true) {
        this.id = id
        this.label = label
        this.enabled = enabled
    }
}
