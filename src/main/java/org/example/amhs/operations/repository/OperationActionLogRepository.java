package org.example.amhs.operations.repository;

import java.util.List;
import org.example.amhs.operations.domain.OperationActionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationActionLogRepository extends JpaRepository<OperationActionLog, Long> {

    List<OperationActionLog> findTop20ByOrderByCreatedAtDescActionLogIdDesc();
}
