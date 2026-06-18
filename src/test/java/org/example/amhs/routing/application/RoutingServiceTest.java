package org.example.amhs.routing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.example.amhs.common.exception.ResourceNotFoundException;
import org.example.amhs.common.exception.RouteNotFoundException;
import org.example.amhs.fab.domain.FabEdge;
import org.example.amhs.fab.domain.FabNode;
import org.example.amhs.fab.domain.NodeType;
import org.example.amhs.fab.repository.FabEdgeRepository;
import org.example.amhs.fab.repository.FabNodeRepository;
import org.example.amhs.routing.domain.RouteResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class RoutingServiceTest {

    @Autowired
    private RoutingService routingService;

    @Autowired
    private FabNodeRepository fabNodeRepository;

    @Autowired
    private FabEdgeRepository fabEdgeRepository;

    @BeforeEach
    void setUp() {
        fabEdgeRepository.deleteAll();
        fabNodeRepository.deleteAll();

        fabNodeRepository.save(new FabNode("A", NodeType.STOCKER, "A", 0, 0, true));
        fabNodeRepository.save(new FabNode("B", NodeType.JUNCTION, "B", 1, 0, true));
        fabNodeRepository.save(new FabNode("C", NodeType.JUNCTION, "C", 2, 0, true));
        fabNodeRepository.save(new FabNode("D", NodeType.EQP, "D", 3, 0, true));

        fabEdgeRepository.save(new FabEdge("E-AB", "A", "B", 10, 10, false));
        fabEdgeRepository.save(new FabEdge("E-BD", "B", "D", 10, 10, false));
        fabEdgeRepository.save(new FabEdge("E-AC", "A", "C", 5, 5, false));
        fabEdgeRepository.save(new FabEdge("E-CD", "C", "D", 30, 30, false));
    }

    @Test
    void 거리_기준_최단_경로를_계산한다() {
        RouteResult result = routingService.findShortestPath("A", "D");

        assertThat(result.pathNodeIds()).containsExactly("A", "B", "D");
        assertThat(result.pathEdgeIds()).containsExactly("E-AB", "E-BD");
        assertThat(result.totalDistanceMeters()).isEqualTo(20.0);
        assertThat(result.estimatedTravelSeconds()).isEqualTo(20);
    }

    @Test
    void 차단된_edge는_경로에서_제외한다() {
        FabEdge edge = fabEdgeRepository.findById("E-AB").orElseThrow();
        edge.block();
        fabEdgeRepository.save(edge);

        RouteResult result = routingService.findShortestPath("A", "D");

        assertThat(result.pathNodeIds()).containsExactly("A", "C", "D");
        assertThat(result.pathEdgeIds()).containsExactly("E-AC", "E-CD");
    }

    @Test
    void 양방향_그래프로_경로를_계산한다() {
        RouteResult result = routingService.findShortestPath("D", "A");

        assertThat(result.pathNodeIds()).containsExactly("D", "B", "A");
        assertThat(result.pathEdgeIds()).containsExactly("E-BD", "E-AB");
    }

    @Test
    void 경로가_없으면_ROUTE_NOT_FOUND를_던진다() {
        fabEdgeRepository.deleteAll();

        assertThatThrownBy(() -> routingService.findShortestPath("A", "D"))
                .isInstanceOf(RouteNotFoundException.class);
    }

    @Test
    void 존재하지_않는_node면_FAB_NODE_NOT_FOUND를_던진다() {
        assertThatThrownBy(() -> routingService.findShortestPath("UNKNOWN", "D"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
