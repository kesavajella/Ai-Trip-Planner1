package com.intellitrip.config;

import com.intellitrip.service.CurrencyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Locale;

/**
 * Exposes the requesting user's locale / currency to every Thymeleaf view so
 * budgets can be rendered in the visitor's local currency (from Accept-Language).
 */
@ControllerAdvice
public class CurrencyAdvice {

    private final CurrencyService currencyService;

    public CurrencyAdvice(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @ModelAttribute("userLocale")
    public Locale userLocale(HttpServletRequest request) {
        return request.getLocale();
    }

    @ModelAttribute("userCurrencyCode")
    public String userCurrencyCode(HttpServletRequest request) {
        return currencyService.currencyCode(request.getLocale());
    }

    @ModelAttribute("usdRate")
    public double usdRate(HttpServletRequest request) {
        return currencyService.usdRate(request.getLocale());
    }
}
