package org.example.amhs.fab.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.example.amhs.common.response.ApiResponse;
import org.example.amhs.fab.domain.FabEdge;
import org.example.amhs.fab.domain.FabNode;
import org.example.amhs.fab.domain.NodeType;
import org.example.amhs.fab.dto.BlockFabEdgeRequest;
import org.example.amhs.fab.dto.FabEdgeStatusResponse;
import org.example.amhs.fab.dto.FabMapResponse;
import org.example.amhs.fab.repository.FabEdgeRepository;
import org.example.amhs.fab.repository.FabNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class FabMapControllerTest {

    @Autowired
    private FabMapController fabMapController;

    @Autowired
    private FabNodeRepository fabNodeRepository;

    @Autowired
    private FabEdgeRepository fabEdgeRepository;

    @BeforeEach
    void setUp() {
        fabEdgeRepository.deleteAll();
        fabNodeRepository.deleteAll();
        fabNodeRepository.save(new FabNode("STOCKER-A", NodeType.STOCKER, "Stocker A", 100, 200, true));
        fabNodeRepository.save(new FabNode("EQP-01", NodeType.EQP, "Equipment 01", 300, 200, true));
        fabEdgeRepository.save(new FabEdge("EDGE-001", "STOCKER-A", "EQP-01", 12.5, 15, false));
    }

    @Test
    void FAB_Map을_조회한다() {
        ApiResponse<FabMapResponse> response = fabMapController.getFabMap();

        assertThat(response.success()).isTrue();
        assertThat(response.data().nodes()).hasSize(2);
        assertThat(response.data().edges()).hasSize(1);
        assertThat(response.data().edges().get(0).blocked()).isFalse();
    }

    @Test
    void edge를_차단하고_해제한다() {
        ApiResponse<FabEdgeStatusResponse> blockResponse =
                fabMapController.blockEdge("EDGE-001", new BlockFabEdgeRequest("테스트 차단"));

        assertThat(blockResponse.success()).isTrue();
        assertThat(blockResponse.data().edgeId()).isEqualTo("EDGE-001");
        assertThat(blockResponse.data().blocked()).isTrue();
        assertThat(blockResponse.data().reason()).isEqualTo("테스트 차단");

        ApiResponse<FabEdgeStatusResponse> unblockResponse = fabMapController.unblockEdge("EDGE-001");
        assertThat(unblockResponse.success()).isTrue();
        assertThat(unblockResponse.data().edgeId()).isEqualTo("EDGE-001");
        assertThat(unblockResponse.data().blocked()).isFalse();
    }
}
