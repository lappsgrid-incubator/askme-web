package org.lappsgrid.eager.askme.web

import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 *
 */
@RestController
class TemplateController {

    @GetMapping(path="/", produces = "text/html")
    String handleGet() {
        return 'index'
    }

    @PostMapping(path="/formAction", produces = "text/html")
    String handlePost(@RequestParam Map<String,String> params, Model model) {
        // Attributes set in the model will be available in the template.  Here we
        // simply pass the question from the form to the model.
        model.addAttribute('question', params.question)
        return 'question'
    }
}
