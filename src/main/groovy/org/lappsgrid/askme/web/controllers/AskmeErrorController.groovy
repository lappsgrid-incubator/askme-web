package org.lappsgrid.askme.web.controllers

import groovy.util.logging.Slf4j
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping

import javax.servlet.RequestDispatcher
import javax.servlet.http.HttpServletRequest

/**
 *
 */
@Slf4j("logger")
//@Controller
class AskmeErrorController { //implements ErrorController {

    @RequestMapping("/error")
    String handleError(HttpServletRequest request, Model model) {
        logger.error("An error has been encountered")
        logger.error(request.toString())


//        HttpStatus
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());

            if(statusCode == HttpStatus.NOT_FOUND.value()) {
                logger.error("404")
//                request.pathInfo
//                request.contextPath
//                request.requestURI
                model.message = "The page ${request.requestURI} could not be found."
                return "error_page";
            }
            else if(statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                logger.error("500 - internal server error")
                model.message = "There was an internal server error."
                return "error_page";
            }
        }
        return "error_page";
    }

//    @Override
    String getErrorPath() {
        return null

    }
}
