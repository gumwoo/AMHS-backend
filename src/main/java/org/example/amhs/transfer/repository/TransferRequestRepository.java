package org.example.amhs.transfer.repository;

import java.time.OffsetDateTime;
import java.util.List;
import org.example.amhs.transfer.domain.TransferRequest;
import org.example.amhs.transfer.domain.TransferRequestStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long>,
        JpaSpecificationExecutor<TransferRequest> {

    List<TransferRequest> findByStatus(TransferRequestStatus status);

    long countByStatus(TransferRequestStatus status);

    List<TransferRequest> findByStatusIn(List<TransferRequestStatus> statuses, Pageable pageable);

    @Query("""
            select t
            from TransferRequest t
            where t.status = org.example.amhs.transfer.domain.TransferRequestStatus.WAITING
            order by
                case t.priority
                    when org.example.amhs.transfer.domain.TransferPriority.URGENT then 1
                    when org.example.amhs.transfer.domain.TransferPriority.HIGH then 2
                    when org.example.amhs.transfer.domain.TransferPriority.NORMAL then 3
                    when org.example.amhs.transfer.domain.TransferPriority.LOW then 4
                    else 5
                end,
                t.requestedAt asc,
                t.requestId asc
            """)
    List<TransferRequest> findSchedulableRequests(Pageable pageable);

    @Query("""
            select
                count(t) as totalRequests,
                sum(case when t.status = org.example.amhs.transfer.domain.TransferRequestStatus.COMPLETED then 1 else 0 end) as completedRequests,
                sum(case when t.status = org.example.amhs.transfer.domain.TransferRequestStatus.FAILED then 1 else 0 end) as failedRequests,
                sum(case when t.status = org.example.amhs.transfer.domain.TransferRequestStatus.CANCELED then 1 else 0 end) as canceledRequests
            from TransferRequest t
            where t.requestedAt >= :from
              and t.requestedAt <= :to
            """)
    TransferRequestSummary summarizeCounts(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    @Query("""
            select
                sum(case when t.status = org.example.amhs.transfer.domain.TransferRequestStatus.WAITING then 1 else 0 end) as waitingTransfers,
                sum(case when t.status = org.example.amhs.transfer.domain.TransferRequestStatus.ASSIGNED then 1 else 0 end) as assignedTransfers,
                sum(case when t.status = org.example.amhs.transfer.domain.TransferRequestStatus.MOVING then 1 else 0 end) as movingTransfers,
                sum(case when t.status = org.example.amhs.transfer.domain.TransferRequestStatus.COMPLETED then 1 else 0 end) as completedTransfers,
                sum(case when t.status = org.example.amhs.transfer.domain.TransferRequestStatus.FAILED then 1 else 0 end) as failedTransfers,
                sum(case when t.status = org.example.amhs.transfer.domain.TransferRequestStatus.CANCELED then 1 else 0 end) as canceledTransfers
            from TransferRequest t
            """)
    TransferStatusSummary summarizeStatusCounts();

    @Query("""
            select t
            from TransferRequest t
            where t.status = org.example.amhs.transfer.domain.TransferRequestStatus.COMPLETED
              and t.requestedAt is not null
              and t.completedAt is not null
              and t.requestedAt >= :from
              and t.requestedAt <= :to
            """)
    List<TransferRequest> findCompletedForAnalytics(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    @Query("""
            select t
            from TransferRequest t
            where t.assignedOhtId is not null
              and t.status in (
                  org.example.amhs.transfer.domain.TransferRequestStatus.COMPLETED,
                  org.example.amhs.transfer.domain.TransferRequestStatus.FAILED
              )
              and t.requestedAt >= :from
              and t.requestedAt <= :to
            """)
    List<TransferRequest> findOhtThroughputTargets(
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    interface TransferRequestSummary {

        Long getTotalRequests();

        Long getCompletedRequests();

        Long getFailedRequests();

        Long getCanceledRequests();
    }

    interface TransferStatusSummary {

        Long getWaitingTransfers();

        Long getAssignedTransfers();

        Long getMovingTransfers();

        Long getCompletedTransfers();

        Long getFailedTransfers();

        Long getCanceledTransfers();
    }
}
