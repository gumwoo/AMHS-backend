package org.example.amhs.transfer.repository;

import java.util.List;
import org.example.amhs.transfer.domain.TransferHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferHistoryRepository extends JpaRepository<TransferHistory, Long> {

    List<TransferHistory> findByRequestIdOrderByOccurredAtAsc(Long requestId);
}
