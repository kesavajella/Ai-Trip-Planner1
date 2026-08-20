package com.intellitrip.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleAllExceptions(Exception ex, HttpServletRequest request) {
        log.error("=== GLOBAL ERROR HANDLER ===");
        log.error("Request URL: {}", request.getRequestURL());
        log.error("Request Method: {}", request.getMethod());
        log.error("Exception Type: {}", ex.getClass().getName());
        log.error("Exception Message: {}", ex.getMessage());
        log.error("Stack Trace:", ex);

        ModelAndView mav = new ModelAndView();
        mav.addObject("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Unknown error");
        mav.addObject("exceptionClass", ex.getClass().getName());
        mav.setViewName("error");
        return mav;
    }
}