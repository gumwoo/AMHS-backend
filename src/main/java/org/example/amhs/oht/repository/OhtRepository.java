package org.example.amhs.oht.repository;

import java.util.List;
import org.example.amhs.oht.domain.Oht;
import org.example.amhs.oht.domain.OhtStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OhtRepository extends JpaRepository<Oht, String> {

    List<Oht> findByStatusAndCurrentRequestIdIsNull(OhtStatus status);

    long countByStatus(OhtStatus status);

    @Modifying
    @Query("""
            update Oht o
            set o.status = org.example.amhs.oht.domain.OhtStatus.RESERVED,
                o.currentRequestId = :requestId,
                o.lastMovedAt = :now
            where o.ohtId = :ohtId
              and o.status = org.example.amhs.oht.domain.OhtStatus.IDLE
              and o.currentRequestId is null
            """)
    int reserveIfIdle(
            @Param("ohtId") String ohtId,
            @Param("requestId") Long requestId,
            @Param("now") java.time.OffsetDateTime now
    );
}
