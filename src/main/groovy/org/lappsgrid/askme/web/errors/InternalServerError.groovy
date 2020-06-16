package org.lappsgrid.askme.web.errors

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 *
 */
class InternalServerError extends ResponseStatusException {
    InternalServerError(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message)
    }
}
