package org.example.amhs.oht.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import org.example.amhs.common.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

class OhtStateTransitionTest {

    private final OffsetDateTime now = OffsetDateTime.parse("2026-06-18T15:30:00+09:00");

    @Test
    void OHT는_IDLE_상태로_생성된다() {
        Oht oht = Oht.create("OHT-01", "STOCKER-A", now);

        assertThat(oht.getStatus()).isEqualTo(OhtStatus.IDLE);
        assertThat(oht.getCurrentNodeId()).isEqualTo("STOCKER-A");
        assertThat(oht.getCurrentRequestId()).isNull();
    }

    @Test
    void IDLE_OHT만_예약할_수_있다() {
        Oht oht = Oht.create("OHT-01", "STOCKER-A", now);

        oht.reserve(1001L, now.plusSeconds(1));

        assertThat(oht.getStatus()).isEqualTo(OhtStatus.RESERVED);
        assertThat(oht.getCurrentRequestId()).isEqualTo(1001L);
        assertThatThrownBy(() -> oht.reserve(1002L, now.plusSeconds(2)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void RESERVED_OHT는_MOVING으로_전이할_수_있다() {
        Oht oht = Oht.create("OHT-01", "STOCKER-A", now);
        oht.reserve(1001L, now.plusSeconds(1));

        oht.startMoving(now.plusSeconds(2));

        assertThat(oht.getStatus()).isEqualTo(OhtStatus.MOVING);
    }

    @Test
    void ERROR_OHT는_예약할_수_없다() {
        Oht oht = Oht.create("OHT-01", "STOCKER-A", now);
        oht.markError(now.plusSeconds(1));

        assertThatThrownBy(() -> oht.reserve(1001L, now.plusSeconds(2)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void 이동_완료_후_OHT를_IDLE로_해제한다() {
        Oht oht = Oht.create("OHT-01", "STOCKER-A", now);
        oht.reserve(1001L, now.plusSeconds(1));
        oht.startMoving(now.plusSeconds(2));
        oht.moveTo("EQP-01", now.plusSeconds(10));

        oht.release(now.plusSeconds(11));

        assertThat(oht.getStatus()).isEqualTo(OhtStatus.IDLE);
        assertThat(oht.getCurrentRequestId()).isNull();
        assertThat(oht.getCurrentNodeId()).isEqualTo("EQP-01");
    }

    @Test
    void ERROR_OHT는_복구하면_IDLE이_된다() {
        Oht oht = Oht.create("OHT-01", "STOCKER-A", now);
        oht.markError(now.plusSeconds(1));

        oht.recover(now.plusSeconds(2));

        assertThat(oht.getStatus()).isEqualTo(OhtStatus.IDLE);
        assertThat(oht.getCurrentRequestId()).isNull();
    }
}
