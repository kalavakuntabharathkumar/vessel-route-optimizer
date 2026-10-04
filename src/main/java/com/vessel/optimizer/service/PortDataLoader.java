package com.vessel.optimizer.service;

import com.vessel.optimizer.model.Port;
import com.vessel.optimizer.repository.PortRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class PortDataLoader implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(PortDataLoader.class);
    private static final String CSV_FILE = "data/ports.csv";
    private static final int BATCH_SIZE = 500;

    private final PortRepository portRepository;

    public PortDataLoader(PortRepository portRepository) {
        this.portRepository = portRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (portRepository.count() > 0) {
            log.info("Port data already loaded, skipping ingestion");
            return;
        }
        loadPortsFromCsv();
    }

    private void loadPortsFromCsv() {
        try (var resource = new ClassPathResource(CSV_FILE);
             var reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
             var parser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                 .setHeader()
                 .setSkipHeaderRecord(true)
                 .setTrim(true)
                 .setIgnoreEmptyLines(true)
                 .build())) {

            List<Port> batch = new ArrayList<>();
            int count = 0;

            for (CSVRecord record : parser) {
                Port port = parsePort(record);
                if (port != null && port.hasValidCoordinates()) {
                    batch.add(port);
                    if (batch.size() >= BATCH_SIZE) {
                        portRepository.saveAll(batch);
                        count += batch.size();
                        batch.clear();
                    }
                }
            }
            if (!batch.isEmpty()) {
                portRepository.saveAll(batch);
                count += batch.size();
            }
            log.info("Loaded {} ports from UN/LOCODE CSV", count);
        } catch (Exception e) {
            log.error("Failed to load port data", e);
            throw new RuntimeException("Port data ingestion failed", e);
        }
    }

    private Port parsePort(CSVRecord record) {
        try {
            String code = record.get("LOCODE").trim().toUpperCase();
            if (code.length() != 5) return null;

            return Port.builder()
                .code(code)
                .name(record.get("Name").trim())
                .country(code.substring(0, 2))
                .latitude(Double.parseDouble(record.get("Latitude")))
                .longitude(Double.parseDouble(record.get("Longitude")))
                .build();
        } catch (Exception e) {
            return null;
        }
    }
}