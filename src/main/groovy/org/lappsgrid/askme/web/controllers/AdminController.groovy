package org.lappsgrid.askme.web.controllers

import org.lappsgrid.askme.web.services.MessageService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

/**
 *
 */
@RestController
@RequestMapping("/admin")
class AdminController {

    @Autowired
    MessageService messages

    @GetMapping(path = "/message", produces = ["application/json"])
    String[] getMessage() {
        return messages.getMessage()
    }

    @PostMapping(path="/message", produces = "application/json")
    Map postMessage(@RequestBody(required = false) String text) {
        if (text == null) {
            messages.setMessage(null)
            return [
                    status: 'ok',
                    length: 0,
                    message: 'Reset message text.'
            ]
        }

        messages.setMessage(URLDecoder.decode(text, 'UTF-8'))
        return [
                status: 'ok',
                length: text.size(),
                message: text
        ]
    }
}
