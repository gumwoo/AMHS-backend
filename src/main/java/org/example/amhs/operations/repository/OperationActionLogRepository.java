package org.example.amhs.operations.repository;

import java.util.List;
import org.example.amhs.operations.domain.OperationActionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OperationActionLogRepository extends JpaRepository<OperationActionLog, Long>, JpaSpecificationExecutor<OperationActionLog> {

    List<OperationActionLog> findTop20ByOrderByCreatedAtDescActionLogIdDesc();
}
