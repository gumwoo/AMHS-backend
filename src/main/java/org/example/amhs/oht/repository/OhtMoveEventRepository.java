package org.example.amhs.oht.repository;

import java.time.OffsetDateTime;
import java.util.List;
import org.example.amhs.oht.domain.OhtMoveEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OhtMoveEventRepository extends JpaRepository<OhtMoveEvent, String> {

    List<OhtMoveEvent> findByRequestIdOrderByOccurredAtAsc(Long requestId);

    @Query("""
            select e
            from OhtMoveEvent e
            where e.occurredAt >= :from
              and e.occurredAt <= :to
            """)
    List<OhtMoveEvent> findForBottleneckAnalytics(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );
}
