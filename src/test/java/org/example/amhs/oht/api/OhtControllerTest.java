package org.example.amhs.oht.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import org.example.amhs.common.response.ApiResponse;
import org.example.amhs.oht.domain.Oht;
import org.example.amhs.oht.domain.OhtStatus;
import org.example.amhs.oht.dto.OhtDetailResponse;
import org.example.amhs.oht.dto.OhtResponse;
import org.example.amhs.oht.repository.OhtRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class OhtControllerTest {

    private final OffsetDateTime now = OffsetDateTime.parse("2026-06-18T15:30:00+09:00");

    @Autowired
    private OhtController ohtController;

    @Autowired
    private OhtRepository ohtRepository;

    @BeforeEach
    void setUp() {
        ohtRepository.deleteAll();
        ohtRepository.save(Oht.create("OHT-01", "STOCKER-A", now.minusMinutes(10)));
        Oht error = Oht.create("OHT-02", "CHARGER-01", now.minusMinutes(20));
        error.markError(now.minusMinutes(1));
        ohtRepository.save(error);
    }

    @Test
    void OHT_목록을_조회한다() {
        ApiResponse<List<OhtResponse>> response = ohtController.getOhts(null, null);

        assertThat(response.success()).isTrue();
        assertThat(response.data()).hasSize(2);
        assertThat(response.data().get(0).ohtId()).isEqualTo("OHT-01");
    }

    @Test
    void 상태로_OHT_목록을_필터링한다() {
        ApiResponse<List<OhtResponse>> response = ohtController.getOhts(OhtStatus.ERROR, null);

        assertThat(response.data()).hasSize(1);
        assertThat(response.data().get(0).ohtId()).isEqualTo("OHT-02");
        assertThat(response.data().get(0).status()).isEqualTo(OhtStatus.ERROR);
    }

    @Test
    void OHT_상세를_조회한다() {
        ApiResponse<OhtDetailResponse> response = ohtController.getOht("OHT-01");

        assertThat(response.success()).isTrue();
        assertThat(response.data().ohtId()).isEqualTo("OHT-01");
        assertThat(response.data().recentMoveEvents()).isEmpty();
    }
}
