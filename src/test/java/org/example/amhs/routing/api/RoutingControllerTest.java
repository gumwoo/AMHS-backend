package org.example.amhs.routing.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.example.amhs.common.response.ApiResponse;
import org.example.amhs.fab.domain.FabEdge;
import org.example.amhs.fab.domain.FabNode;
import org.example.amhs.fab.domain.NodeType;
import org.example.amhs.fab.repository.FabEdgeRepository;
import org.example.amhs.fab.repository.FabNodeRepository;
import org.example.amhs.routing.dto.RouteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class RoutingControllerTest {

    @Autowired
    private RoutingController routingController;

    @Autowired
    private FabNodeRepository fabNodeRepository;

    @Autowired
    private FabEdgeRepository fabEdgeRepository;

    @BeforeEach
    void setUp() {
        fabEdgeRepository.deleteAll();
        fabNodeRepository.deleteAll();
        fabNodeRepository.save(new FabNode("A", NodeType.STOCKER, "A", 0, 0, true));
        fabNodeRepository.save(new FabNode("B", NodeType.EQP, "B", 1, 0, true));
        fabEdgeRepository.save(new FabEdge("E-AB", "A", "B", 12.5, 15, false));
    }

    @Test
    void 최단_경로_API를_조회한다() {
        ApiResponse<RouteResponse> response = routingController.findShortestRoute("A", "B");

        assertThat(response.success()).isTrue();
        assertThat(response.data().sourceNodeId()).isEqualTo("A");
        assertThat(response.data().destinationNodeId()).isEqualTo("B");
        assertThat(response.data().totalDistanceMeters()).isEqualTo(12.5);
        assertThat(response.data().estimatedTravelSeconds()).isEqualTo(15);
        assertThat(response.data().path()).containsExactly("A", "B");
        assertThat(response.data().edges()).containsExactly("E-AB");
    }
}
