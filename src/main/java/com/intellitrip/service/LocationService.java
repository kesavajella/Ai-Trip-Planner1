package com.intellitrip.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);
    private static final int DEFAULT_LIMIT = 10;
    private static final int MIN_QUERY_LENGTH = 1;

    /**
     * Maximum number of location entries kept in the in-memory index.
     * The full countries+states+cities JSON has hundreds of thousands of
     * entries and is the #1 cause of the 512MB Out-Of-Memory on Render's
     * free tier. Capping it keeps RAM usage bounded while still supporting
     * country/state/city autocomplete.
     */
    private static final int MAX_INDEX_ENTRIES = 60_000;

    private final ObjectMapper objectMapper;
    private final String jsonPath;
    private volatile List<Map<String, Object>> index = null;
    private final ReentrantLock loadLock = new ReentrantLock();

    public LocationService(ObjectMapper objectMapper,
                           @Value("${app.locations.json-path:static/data/countries+states+cities.json}") String jsonPath) {
        this.objectMapper = objectMapper;
        this.jsonPath = jsonPath;
    }

    private List<Map<String, Object>> loadIndex() {
        if (index != null) return index;
        loadLock.lock();
        try {
            if (index != null) return index;
            index = buildIndex();
            log.info("Location index loaded: {} entries from {}", index.size(), jsonPath);
            return index;
        } finally {
            loadLock.unlock();
        }
    }

    public List<Map<String, Object>> getIndex() {
        return loadIndex();
    }

    private List<Map<String, Object>> buildIndex() {
        List<Map<String, Object>> tmp = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        boolean capped = false;
        try (InputStream in = openStream()) {
            JsonNode root = objectMapper.readTree(in);
            if (!root.isArray()) return tmp;
            for (JsonNode country : root) {
                String cName = textOf(country, "name");
                String cCode = textOf(country, "iso2");
                String cCurrency = textOf(country, "currency");
                addEntry(tmp, seen, "country", cName, cName, null, cCode, cCurrency);
                JsonNode states = country.get("states");
                if (states != null && states.isArray()) {
                    for (JsonNode state : states) {
                        String sName = textOf(state, "name");
                        addEntry(tmp, seen, "state", sName, cName, sName, cCode, cCurrency);
                        JsonNode cities = state.get("cities");
                        if (cities != null && cities.isArray()) {
                            for (JsonNode city : cities) {
                                String cityName = textOf(city, "name");
                                if (tmp.size() >= MAX_INDEX_ENTRIES) {
                                    capped = true;
                                    break;
                                }
                                addEntry(tmp, seen, "city", cityName, cName, sName, cCode, cCurrency);
                            }
                        }
                        if (capped) break;
                    }
                }
                if (capped) break;
            }
        } catch (Exception e) {
            log.error("Failed to load location index from {}", jsonPath, e);
        }
        if (capped) {
            log.warn("Location index reached cap of {} entries; some cities are not indexed to keep memory bounded.",
                    MAX_INDEX_ENTRIES);
        }
        return tmp;
    }

    private InputStream openStream() throws IOException {
        File file = new File(jsonPath);
        if (file.exists()) return new BufferedInputStream(new FileInputStream(file));

        File cwdFile = new File(System.getProperty("user.dir"), jsonPath);
        if (cwdFile.exists()) return new BufferedInputStream(new FileInputStream(cwdFile));

        File parent = new File(System.getProperty("user.dir"));
        File rootFile = parent.getParentFile();
        while (rootFile != null) {
            File candidate = new File(rootFile, jsonPath);
            if (candidate.exists()) return new BufferedInputStream(new FileInputStream(candidate));
            rootFile = rootFile.getParentFile();
        }

        ClassPathResource cp = new ClassPathResource("static/data/countries+states+cities.json");
        if (cp.exists()) return cp.getInputStream();

        throw new FileNotFoundException("Location data file not found at: " + jsonPath);
    }

    private void addEntry(List<Map<String, Object>> list, Set<String> seen,
                         String type, String name, String country, String state,
                         String countryCode, String currencyCode) {
        if (name == null || name.isBlank()) return;
        String key = type + "|" + name.toLowerCase(Locale.ROOT) + "|"
                + (country == null ? "" : country.toLowerCase(Locale.ROOT)) + "|"
                + (state == null ? "" : state.toLowerCase(Locale.ROOT));
        if (!seen.add(key)) return;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("name", name);
        m.put("country", country == null ? "" : country);
        m.put("state", state == null ? "" : state);
        m.put("countryCode", countryCode == null ? "" : countryCode);
        m.put("currencyCode", currencyCode == null ? "" : currencyCode);
        list.add(m);
    }

    private String textOf(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null ? v.asText() : "";
    }

    public List<Map<String, Object>> search(String query, int limit) {
        if (query == null) return Collections.emptyList();
        String q = query.toLowerCase(Locale.ROOT).trim();
        if (q.length() < MIN_QUERY_LENGTH) return Collections.emptyList();
        List<Map<String, Object>> data = loadIndex();
        if (data.isEmpty()) return Collections.emptyList();
        int max = limit <= 0 ? DEFAULT_LIMIT : limit;
        return data.stream()
                .filter(m -> {
                    // Only show countries, states, and cities (no regions/subregions/other types)
                    String type = (String) m.get("type");
                    if (type == null || !(type.equals("country") || type.equals("state") || type.equals("city"))) {
                        return false;
                    }
                    String name = (String) m.get("name");
                    if (name == null || name.isBlank()) return false;
                    String ctry = (String) m.get("country");
                    String st = (String) m.get("state");
                    return name.toLowerCase(Locale.ROOT).contains(q)
                            || (ctry != null && !ctry.isBlank() && ctry.toLowerCase(Locale.ROOT).contains(q))
                            || (st != null && !st.isBlank() && st.toLowerCase(Locale.ROOT).contains(q));
                })
                .limit(max)
                .collect(Collectors.toList());
    }
}

