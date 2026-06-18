package org.example.amhs.transfer.repository;

import org.example.amhs.transfer.domain.TransferRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long>,
        JpaSpecificationExecutor<TransferRequest> {
}
