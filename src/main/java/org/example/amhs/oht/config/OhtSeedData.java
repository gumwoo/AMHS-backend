package org.example.amhs.oht.config;

import java.time.OffsetDateTime;
import org.example.amhs.oht.domain.Oht;
import org.example.amhs.oht.repository.OhtRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Order(2)
@Component
public class OhtSeedData implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(OhtSeedData.class);

    private final OhtRepository ohtRepository;

    public OhtSeedData(OhtRepository ohtRepository) {
        this.ohtRepository = ohtRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (ohtRepository.count() > 0) {
            return;
        }

        OffsetDateTime baseTime = OffsetDateTime.parse("2026-06-18T15:00:00+09:00");
        ohtRepository.save(Oht.create("OHT-01", "STOCKER-A", baseTime.minusMinutes(10)));
        ohtRepository.save(Oht.create("OHT-02", "CHARGER-01", baseTime.minusMinutes(20)));
        ohtRepository.save(Oht.create("OHT-03", "STOCKER-B", baseTime.minusMinutes(30)));

        log.info("기본 OHT seed data를 생성했습니다. ohts={}", ohtRepository.count());
    }
}
