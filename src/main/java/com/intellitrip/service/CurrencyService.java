package com.intellitrip.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Formats USD amounts in the requesting user's locale currency.
 *
 * <p>Exchange rates are fetched live from the Frankfurter API
 * ({@code https://api.frankfurter.dev}) and cached for a configurable
 * interval.  If the API is unavailable the service transparently falls
 * back to a built-in table of static rates so currency formatting never
 * breaks.</p>
 */
@Service
public class CurrencyService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyService.class);

    /**
     * Static fallback rates used when the Frankfurter API cannot be reached.
     * These are USD-to-X multipliers (e.g. 1 USD = 83.5 INR).
     */
    private static final Map<String, Double> FALLBACK_RATES = new HashMap<>();

    static {
        FALLBACK_RATES.put("USD", 1.0);
        FALLBACK_RATES.put("EUR", 0.92);
        FALLBACK_RATES.put("GBP", 0.79);
        FALLBACK_RATES.put("JPY", 154.0);
        FALLBACK_RATES.put("INR", 83.0);
        FALLBACK_RATES.put("AUD", 1.52);
        FALLBACK_RATES.put("CAD", 1.35);
        FALLBACK_RATES.put("CHF", 0.88);
        FALLBACK_RATES.put("CNY", 7.2);
        FALLBACK_RATES.put("SGD", 1.34);
        FALLBACK_RATES.put("AED", 3.67);
        FALLBACK_RATES.put("MXN", 16.9);
        FALLBACK_RATES.put("BRL", 5.2);
        FALLBACK_RATES.put("ZAR", 18.2);
        FALLBACK_RATES.put("KRW", 1380.0);
        FALLBACK_RATES.put("NZD", 1.64);
        FALLBACK_RATES.put("HKD", 7.8);
        FALLBACK_RATES.put("SEK", 10.4);
        FALLBACK_RATES.put("NOK", 10.5);
        FALLBACK_RATES.put("DKK", 6.9);
        FALLBACK_RATES.put("THB", 36.5);
        FALLBACK_RATES.put("IDR", 16500.0);
        FALLBACK_RATES.put("TRY", 32.5);
        FALLBACK_RATES.put("PLN", 3.9);
        FALLBACK_RATES.put("RUB", 90.0);
    }

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String frankfurterApiUrl;
    private final long refreshIntervalMs;

    /** Live rates fetched from Frankfurter (null until first successful fetch). */
    private volatile Map<String, Double> liveRates = null;
    /** Epoch millis of the last successful fetch. */
    private volatile long lastFetchTime = 0;
    private final ReentrantLock refreshLock = new ReentrantLock();

    public CurrencyService(@Value("${app.frankfurter.api-url:https://api.frankfurter.dev/v1/latest?base=USD}") String frankfurterApiUrl,
                           @Value("${app.frankfurter.refresh-interval-ms:3600000}") long refreshIntervalMs) {
        this.frankfurterApiUrl = frankfurterApiUrl;
        this.refreshIntervalMs = refreshIntervalMs;
    }

    /**
     * Eagerly fetch rates on startup so the first requests already benefit
     * from live data when available.
     */
    @PostConstruct
    public void init() {
        log.info("CurrencyService initialized — Frankfurter API URL: {}", frankfurterApiUrl);
        doFetchLiveRates();
    }

    /**
     * Periodically refresh live exchange rates from the Frankfurter API.
     */
    @Scheduled(fixedRateString = "${app.frankfurter.refresh-interval-ms:3600000}")
    public void refreshLiveRates() {
        doFetchLiveRates();
    }

    /**
     * Call the Frankfurter API and update the live-rates cache.
     * On failure the existing cached rates (or fallback) remain in effect.
     */
    private void doFetchLiveRates() {
        refreshLock.lock();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(frankfurterApiUrl))
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = MAPPER.readTree(response.body());
                JsonNode ratesNode = root.get("rates");
                if (ratesNode != null && ratesNode.isObject()) {
                    Map<String, Double> rates = new HashMap<>();
                    ratesNode.fields().forEachRemaining(entry ->
                            rates.put(entry.getKey(), entry.getValue().asDouble()));
                    liveRates = rates;
                    lastFetchTime = System.currentTimeMillis();
                    int sourceCount = FALLBACK_RATES.size();
                    log.info("Fetched {} live exchange rates from Frankfurter (base: USD, {} fallback rates available)",
                            rates.size(), sourceCount);
                } else {
                    log.warn("Frankfurter API response did not contain expected 'rates' object");
                }
            } else {
                log.warn("Frankfurter API returned HTTP {} — using cached/fallback rates", response.statusCode());
            }
        } catch (IOException e) {
            log.warn("IOException fetching exchange rates from Frankfurter — using cached/fallback rates: {}", e.getMessage());
        } catch (InterruptedException e) {
            log.warn("Interrupted while fetching exchange rates — using cached/fallback rates");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Unexpected error fetching exchange rates from Frankfurter — using cached/fallback rates: {}", e.getMessage());
        } finally {
            refreshLock.unlock();
        }
    }

    /**
     * @return the USD-to-{@code currencyCode} multiplier from live rates
     *         when available, otherwise from the static fallback table.
     */
    private double rateForCurrency(String currencyCode) {
        if (liveRates != null) {
            Double live = liveRates.get(currencyCode);
            if (live != null) return live;
        }
        return FALLBACK_RATES.getOrDefault(currencyCode, 1.0);
    }

    /**
     * Resolve the ISO 4217 currency code for the given locale.
     */
    public String currencyCode(Locale locale) {
        if (locale == null || locale.getCountry() == null || locale.getCountry().isBlank()) {
            return "USD";
        }
        try {
            Currency c = Currency.getInstance(locale);
            if (c != null) return c.getCurrencyCode();
        } catch (Exception ignored) {
        }
        return "USD";
    }

    /**
     * USD-to-local multiplier for the currency implied by the locale.
     */
    public double usdRate(Locale locale) {
        return rateForCurrency(currencyCode(locale));
    }

    /**
     * @deprecated Use {@link #currencySymbol(Locale)} for locale-based lookups
     *             or {@link #rateForCurrency(String)} for code-based lookups.
     */
    public String currencySymbol(Locale locale) {
        try {
            return Currency.getInstance(currencyCode(locale)).getSymbol(locale);
        } catch (Exception e) {
            return "$";
        }
    }

    /**
     * Convert a USD amount to the locale's currency and format it
     * with the appropriate symbol and grouping.
     */
    public String format(double usd, Locale locale) {
        String code = currencyCode(locale);
        double local = usd * usdRate(locale);
        Currency currency = Currency.getInstance(code);
        Locale loc = (locale != null && locale.getCountry() != null && !locale.getCountry().isBlank())
                ? locale : Locale.US;
        NumberFormat nf = NumberFormat.getCurrencyInstance(loc);
        nf.setCurrency(currency);
        nf.setMaximumFractionDigits(currency.getDefaultFractionDigits());
        nf.setMinimumFractionDigits(currency.getDefaultFractionDigits());
        return nf.format(local);
    }

/**
     * Convert a USD amount to a target ISO 4217 currency code.
     *
     * @param usdAmount     the amount in USD
     * @param currencyCode  the target ISO 4217 code (e.g. "INR", "EUR")
     * @return the equivalent amount in the target currency
     */
    public double convertUsdToLocal(double usdAmount, String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return usdAmount;
        }
        return usdAmount * rateForCurrency(currencyCode);
    }

    /**
     * Public accessor for the USD-to-{@code currencyCode} multiplier.
     */
    public double usdRateForCurrency(String currencyCode) {
        return rateForCurrency(currencyCode);
    }

    /**
     * Resolve the ISO 4217 currency code for a country's alpha-2 code
     * (e.g. "IN" -> "INR") by looking it up in countries.json.
     * Returns null if not found.
     */
    public String currencyCodeForCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) return null;
        try (InputStream in = new ClassPathResource("static/data/countries.json").getInputStream()) {
            List<Map<String, Object>> countries = MAPPER.readValue(in, List.class);
            for (Map<String, Object> country : countries) {
                String alpha2 = (String) country.getOrDefault("alpha2", "");
                if (countryCode.equalsIgnoreCase(alpha2)) {
                    Object currency = country.get("currency");
                    if (currency instanceof Map) {
                        return (String) ((Map<?, ?>) currency).get("code");
                    }
                    return (String) currency;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to lookup currency for country code: {}", countryCode, e);
        }
        return null;
    }

    /**
     * Convert a USD amount to a target ISO 4217 currency and return a
     * locale-aware formatted string with currency symbol.
     *
     * @param usdAmount     the amount in USD
     * @param currencyCode  the target ISO 4217 code (e.g. "INR", "EUR")
     * @param locale        locale used for symbol/number formatting
     * @return formatted string, e.g. "&#8377;83,500 INR"
     */
    public String convertAndFormat(double usdAmount, String currencyCode, Locale locale) {
        double local = convertUsdToLocal(usdAmount, currencyCode);
        Currency currency;
        try {
            currency = Currency.getInstance(currencyCode);
        } catch (Exception e) {
            currency = Currency.getInstance("USD");
        }
        Locale loc = (locale != null && locale.getCountry() != null && !locale.getCountry().isBlank())
                ? locale : Locale.US;
        NumberFormat nf = NumberFormat.getCurrencyInstance(loc);
        nf.setCurrency(currency);
        nf.setMaximumFractionDigits(currency.getDefaultFractionDigits());
        nf.setMinimumFractionDigits(currency.getDefaultFractionDigits());
        return nf.format(local);
    }

    public String formatInCurrency(double amount, String currencyCode, Locale locale) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return format(amount, locale);
        }
        Currency currency;
        try {
            currency = Currency.getInstance(currencyCode);
        } catch (Exception e) {
            currency = Currency.getInstance("USD");
        }
        Locale loc = (locale != null && locale.getCountry() != null && !locale.getCountry().isBlank())
                ? locale : Locale.US;
        NumberFormat nf = NumberFormat.getCurrencyInstance(loc);
        nf.setCurrency(currency);
        nf.setMaximumFractionDigits(currency.getDefaultFractionDigits());
        nf.setMinimumFractionDigits(currency.getDefaultFractionDigits());
        return nf.format(amount);
    }
}
