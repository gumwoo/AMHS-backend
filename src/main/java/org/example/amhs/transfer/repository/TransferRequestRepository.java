package org.example.amhs.transfer.repository;

import java.util.List;
import org.example.amhs.transfer.domain.TransferRequest;
import org.example.amhs.transfer.domain.TransferRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long>,
        JpaSpecificationExecutor<TransferRequest> {

    List<TransferRequest> findByStatus(TransferRequestStatus status);

    long countByStatus(TransferRequestStatus status);
}
