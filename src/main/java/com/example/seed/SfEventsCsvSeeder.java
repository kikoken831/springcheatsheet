package com.example.seed;

import com.example.entity.Event;
import com.example.repository.EventRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class SfEventsCsvSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SfEventsCsvSeeder.class);
    private final EventRepository repo;

    public SfEventsCsvSeeder(EventRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) throws Exception {
        if (repo.count() > 0) return;

        ClassPathResource resource = new ClassPathResource("datasets/sf_events.csv");
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {

            CSVFormat fmt = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build();

            List<Event> batch = new ArrayList<>();

            for (CSVRecord r : fmt.parse(reader)) {
                Event e = new Event();
                e.setOrganizerName(r.get("org_name"));
                e.setName(r.get("event_name"));
                e.setDescription(r.get("event_description"));
                e.setCategory(r.get("events_category"));
                e.setLocation(firstNonBlank(r.get("site_location_name"), r.get("site_address")));
                e.setStartDate(parseDateSafe(r.get("event_start_date")));
                e.setEndDate(parseDateSafe(r.get("event_end_date")));
                e.setFee(Boolean.valueOf(r.get("fee")));
                e.setAdmissionPrice(parseMoneySafe(r.get("admission_price")));
                e.setLatitude(parseDoubleSafe(r.get("latitude")));
                e.setLongitude(parseDoubleSafe(r.get("longitude")));
                e.setStatus("ACTIVE");
                e.setCreatedAt(LocalDateTime.now());
                batch.add(e);

                if (batch.size() == 500) {
                    repo.saveAll(batch);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) repo.saveAll(batch);
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

    private static LocalDate parseDateSafe(String s) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu/MM/dd hh:mm:ss a", Locale.US);
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s,formatter);
        } catch (Exception ignore) {
            System.err.println("Unable to parse date: " + s);
            log.error("e: ", ignore);
            return null;
        }
    }

    private static BigDecimal parseMoneySafe(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            String cleaned = s.replaceAll("[^0-9.\\-]", "");
            if (cleaned.isBlank()) return null;
            return new BigDecimal(cleaned);
        } catch (Exception ignore) { return null; }
    }

    private static Double parseDoubleSafe(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Double.valueOf(s); } catch (Exception ignore) { return null; }
    }
}
