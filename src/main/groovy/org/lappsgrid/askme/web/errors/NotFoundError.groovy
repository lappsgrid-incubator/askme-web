package org.lappsgrid.askme.web.errors

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 *
 */
class NotFoundError extends ResponseStatusException {
    NotFoundError(String message) {
        super(HttpStatus.NOT_FOUND, message)
    }
}
