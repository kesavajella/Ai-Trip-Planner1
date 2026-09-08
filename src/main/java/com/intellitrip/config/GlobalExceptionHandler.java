package com.intellitrip.config;

import com.intellitrip.exception.GeminiQuotaExceededException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(GeminiQuotaExceededException.class)
    public Object handleGeminiQuotaExceeded(GeminiQuotaExceededException ex, HttpServletRequest request) {
        log.warn("Gemini Quota Exceeded on [{} {}]: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        String uri = request.getRequestURI();
        String acceptHeader = request.getHeader("Accept");
        if ((uri != null && uri.startsWith("/api/")) || (acceptHeader != null && acceptHeader.contains("application/json"))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "error", "Daily limit reached, please try again shortly",
                            "details", ex.getMessage() != null ? ex.getMessage() : "Gemini API rate limit or quota exceeded"
                    ));
        }

        ModelAndView mav = new ModelAndView();
        mav.setStatus(HttpStatus.TOO_MANY_REQUESTS);
        mav.addObject("errorMessage", "Daily limit reached, please try again shortly");
        mav.addObject("exceptionClass", ex.getClass().getName());
        mav.setViewName("error");
        return mav;
    }

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