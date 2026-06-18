package org.example.amhs.dispatch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import org.example.amhs.common.exception.BusinessException;
import org.example.amhs.dispatch.dto.DispatchCandidateResponse;
import org.example.amhs.fab.domain.FabEdge;
import org.example.amhs.fab.domain.FabNode;
import org.example.amhs.fab.domain.NodeType;
import org.example.amhs.fab.repository.FabEdgeRepository;
import org.example.amhs.fab.repository.FabNodeRepository;
import org.example.amhs.oht.domain.Oht;
import org.example.amhs.oht.repository.OhtRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class DispatchServiceTest {

    private final OffsetDateTime now = OffsetDateTime.parse("2026-06-18T15:30:00+09:00");

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private OhtRepository ohtRepository;

    @Autowired
    private FabNodeRepository fabNodeRepository;

    @Autowired
    private FabEdgeRepository fabEdgeRepository;

    @BeforeEach
    void setUp() {
        ohtRepository.deleteAll();
        fabEdgeRepository.deleteAll();
        fabNodeRepository.deleteAll();

        fabNodeRepository.save(new FabNode("SOURCE", NodeType.STOCKER, "Source", 0, 0, true));
        fabNodeRepository.save(new FabNode("NEAR", NodeType.JUNCTION, "Near", 1, 0, true));
        fabNodeRepository.save(new FabNode("FAR", NodeType.JUNCTION, "Far", 2, 0, true));
        fabNodeRepository.save(new FabNode("ISOLATED", NodeType.JUNCTION, "Isolated", 3, 0, true));

        fabEdgeRepository.save(new FabEdge("EDGE-NEAR", "NEAR", "SOURCE", 5, 5, false));
        fabEdgeRepository.save(new FabEdge("EDGE-FAR", "FAR", "SOURCE", 20, 20, false));
    }

    @Test
    void 출발지까지_가장_가까운_IDLE_OHT를_선택한다() {
        ohtRepository.save(Oht.create("OHT-FAR", "FAR", now.minusMinutes(30)));
        ohtRepository.save(Oht.create("OHT-NEAR", "NEAR", now.minusMinutes(10)));

        DispatchCandidateResponse selected = dispatchService.selectBestOht("SOURCE");

        assertThat(selected.ohtId()).isEqualTo("OHT-NEAR");
        assertThat(selected.distanceToSourceMeters()).isEqualTo(5);
        assertThat(selected.estimatedTravelSeconds()).isEqualTo(5);
    }

    @Test
    void RESERVED_MOVING_ERROR_OHT는_후보에서_제외한다() {
        Oht reserved = Oht.create("OHT-RESERVED", "NEAR", now.minusMinutes(30));
        reserved.reserve(1001L, now.minusMinutes(1));
        Oht moving = Oht.create("OHT-MOVING", "NEAR", now.minusMinutes(20));
        moving.reserve(1002L, now.minusMinutes(2));
        moving.startMoving(now.minusMinutes(1));
        Oht error = Oht.create("OHT-ERROR", "NEAR", now.minusMinutes(10));
        error.markError(now.minusMinutes(1));
        Oht idle = Oht.create("OHT-IDLE", "FAR", now.minusMinutes(5));

        ohtRepository.save(reserved);
        ohtRepository.save(moving);
        ohtRepository.save(error);
        ohtRepository.save(idle);

        DispatchCandidateResponse selected = dispatchService.selectBestOht("SOURCE");

        assertThat(selected.ohtId()).isEqualTo("OHT-IDLE");
    }

    @Test
    void 경로가_없는_OHT는_후보에서_제외한다() {
        ohtRepository.save(Oht.create("OHT-ISOLATED", "ISOLATED", now.minusMinutes(30)));
        ohtRepository.save(Oht.create("OHT-FAR", "FAR", now.minusMinutes(10)));

        DispatchCandidateResponse selected = dispatchService.selectBestOht("SOURCE");

        assertThat(selected.ohtId()).isEqualTo("OHT-FAR");
    }

    @Test
    void 거리가_같으면_lastMovedAt이_오래된_OHT를_선택한다() {
        ohtRepository.save(Oht.create("OHT-NEW", "NEAR", now.minusMinutes(5)));
        ohtRepository.save(Oht.create("OHT-OLD", "NEAR", now.minusMinutes(30)));

        DispatchCandidateResponse selected = dispatchService.selectBestOht("SOURCE");

        assertThat(selected.ohtId()).isEqualTo("OHT-OLD");
    }

    @Test
    void 거리와_lastMovedAt이_같으면_ohtId_오름차순으로_선택한다() {
        ohtRepository.save(Oht.create("OHT-B", "NEAR", now.minusMinutes(10)));
        ohtRepository.save(Oht.create("OHT-A", "NEAR", now.minusMinutes(10)));

        DispatchCandidateResponse selected = dispatchService.selectBestOht("SOURCE");

        assertThat(selected.ohtId()).isEqualTo("OHT-A");
    }

    @Test
    void 배정_가능한_OHT가_없으면_예외를_던진다() {
        ohtRepository.save(Oht.create("OHT-ISOLATED", "ISOLATED", now.minusMinutes(10)));

        assertThatThrownBy(() -> dispatchService.selectBestOht("SOURCE"))
                .isInstanceOf(BusinessException.class);
    }
}
