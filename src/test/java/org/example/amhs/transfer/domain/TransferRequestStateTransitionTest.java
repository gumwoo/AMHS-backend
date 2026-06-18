package org.example.amhs.transfer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import org.example.amhs.common.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

class TransferRequestStateTransitionTest {

    private final OffsetDateTime now = OffsetDateTime.parse("2026-06-18T15:30:00+09:00");

    @Test
    void 반송_요청은_WAITING_상태로_생성된다() {
        TransferRequest request = TransferRequest.create("STOCKER-A", "EQP-01", null, now);

        assertThat(request.getStatus()).isEqualTo(TransferRequestStatus.WAITING);
        assertThat(request.getPriority()).isEqualTo(TransferPriority.NORMAL);
        assertThat(request.getRequestedAt()).isEqualTo(now);
    }

    @Test
    void 출발지와_도착지가_같으면_생성할_수_없다() {
        assertThatThrownBy(() -> TransferRequest.create("EQP-01", "EQP-01", TransferPriority.HIGH, now))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void WAITING에서_ASSIGNED로_전이할_수_있다() {
        TransferRequest request = TransferRequest.create("STOCKER-A", "EQP-01", TransferPriority.HIGH, now);

        request.assign("OHT-01", now.plusSeconds(2));

        assertThat(request.getStatus()).isEqualTo(TransferRequestStatus.ASSIGNED);
        assertThat(request.getAssignedOhtId()).isEqualTo("OHT-01");
        assertThat(request.getAssignedAt()).isEqualTo(now.plusSeconds(2));
    }

    @Test
    void WAITING에서_MOVING으로_바로_전이할_수_없다() {
        TransferRequest request = TransferRequest.create("STOCKER-A", "EQP-01", TransferPriority.NORMAL, now);

        assertThatThrownBy(() -> request.startMoving(now.plusSeconds(1)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void ASSIGNED에서_MOVING으로_전이할_수_있다() {
        TransferRequest request = TransferRequest.create("STOCKER-A", "EQP-01", TransferPriority.NORMAL, now);
        request.assign("OHT-01", now.plusSeconds(1));

        request.startMoving(now.plusSeconds(3));

        assertThat(request.getStatus()).isEqualTo(TransferRequestStatus.MOVING);
        assertThat(request.getStartedAt()).isEqualTo(now.plusSeconds(3));
    }

    @Test
    void MOVING에서_COMPLETED로_전이할_수_있다() {
        TransferRequest request = TransferRequest.create("STOCKER-A", "EQP-01", TransferPriority.NORMAL, now);
        request.assign("OHT-01", now.plusSeconds(1));
        request.startMoving(now.plusSeconds(3));

        request.complete(now.plusSeconds(30));

        assertThat(request.getStatus()).isEqualTo(TransferRequestStatus.COMPLETED);
        assertThat(request.getCompletedAt()).isEqualTo(now.plusSeconds(30));
    }

    @Test
    void COMPLETED에서_MOVING으로_되돌릴_수_없다() {
        TransferRequest request = TransferRequest.create("STOCKER-A", "EQP-01", TransferPriority.NORMAL, now);
        request.assign("OHT-01", now.plusSeconds(1));
        request.startMoving(now.plusSeconds(3));
        request.complete(now.plusSeconds(30));

        assertThatThrownBy(() -> request.startMoving(now.plusSeconds(31)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void WAITING과_ASSIGNED만_취소할_수_있다() {
        TransferRequest waitingRequest = TransferRequest.create("STOCKER-A", "EQP-01", TransferPriority.NORMAL, now);
        waitingRequest.cancel("운영자 취소", now.plusSeconds(1));

        TransferRequest movingRequest = TransferRequest.create("STOCKER-A", "EQP-02", TransferPriority.NORMAL, now);
        movingRequest.assign("OHT-01", now.plusSeconds(1));
        movingRequest.startMoving(now.plusSeconds(2));

        assertThat(waitingRequest.getStatus()).isEqualTo(TransferRequestStatus.CANCELED);
        assertThatThrownBy(() -> movingRequest.cancel("운영자 취소", now.plusSeconds(3)))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
