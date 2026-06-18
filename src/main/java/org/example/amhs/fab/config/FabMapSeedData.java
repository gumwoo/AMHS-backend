package org.example.amhs.fab.config;

import org.example.amhs.fab.domain.FabEdge;
import org.example.amhs.fab.domain.FabNode;
import org.example.amhs.fab.domain.NodeType;
import org.example.amhs.fab.repository.FabEdgeRepository;
import org.example.amhs.fab.repository.FabNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Order(1)
@Component
public class FabMapSeedData implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(FabMapSeedData.class);

    private final FabNodeRepository fabNodeRepository;
    private final FabEdgeRepository fabEdgeRepository;

    public FabMapSeedData(
            FabNodeRepository fabNodeRepository,
            FabEdgeRepository fabEdgeRepository
    ) {
        this.fabNodeRepository = fabNodeRepository;
        this.fabEdgeRepository = fabEdgeRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (fabNodeRepository.count() > 0 || fabEdgeRepository.count() > 0) {
            return;
        }

        fabNodeRepository.save(new FabNode("STOCKER-A", NodeType.STOCKER, "Stocker A", 100, 200, true));
        fabNodeRepository.save(new FabNode("STOCKER-B", NodeType.STOCKER, "Stocker B", 100, 360, true));
        fabNodeRepository.save(new FabNode("JUNCTION-01", NodeType.JUNCTION, "Junction 01", 260, 280, true));
        fabNodeRepository.save(new FabNode("JUNCTION-02", NodeType.JUNCTION, "Junction 02", 420, 280, true));
        fabNodeRepository.save(new FabNode("EQP-01", NodeType.EQP, "Equipment 01", 580, 180, true));
        fabNodeRepository.save(new FabNode("EQP-02", NodeType.EQP, "Equipment 02", 580, 380, true));
        fabNodeRepository.save(new FabNode("CHARGER-01", NodeType.CHARGER, "Charger 01", 260, 440, true));

        fabEdgeRepository.save(new FabEdge("EDGE-001", "STOCKER-A", "JUNCTION-01", 12.5, 15, false));
        fabEdgeRepository.save(new FabEdge("EDGE-002", "STOCKER-B", "JUNCTION-01", 15.0, 18, false));
        fabEdgeRepository.save(new FabEdge("EDGE-003", "JUNCTION-01", "JUNCTION-02", 20.0, 24, false));
        fabEdgeRepository.save(new FabEdge("EDGE-004", "JUNCTION-02", "EQP-01", 14.0, 17, false));
        fabEdgeRepository.save(new FabEdge("EDGE-005", "JUNCTION-02", "EQP-02", 16.0, 20, false));
        fabEdgeRepository.save(new FabEdge("EDGE-006", "CHARGER-01", "JUNCTION-01", 10.0, 12, false));

        log.info("기본 FAB Map seed data를 생성했습니다. nodes={}, edges={}",
                fabNodeRepository.count(),
                fabEdgeRepository.count());
    }
}
