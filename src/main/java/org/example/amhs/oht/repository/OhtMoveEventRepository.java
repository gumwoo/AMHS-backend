package org.example.amhs.oht.repository;

import java.util.List;
import org.example.amhs.oht.domain.OhtMoveEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OhtMoveEventRepository extends JpaRepository<OhtMoveEvent, String> {

    List<OhtMoveEvent> findByRequestIdOrderByOccurredAtAsc(Long requestId);
}
