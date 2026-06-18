package org.example.amhs.operations.dto;

public record OperationsStatusCountResponse(
        long waitingTransfers,
        long assignedTransfers,
        long movingTransfers,
        long completedTransfers,
        long failedTransfers,
        long canceledTransfers,
        long idleOhts,
        long reservedOhts,
        long movingOhts,
        long errorOhts,
        long blockedEdges
) {
}
