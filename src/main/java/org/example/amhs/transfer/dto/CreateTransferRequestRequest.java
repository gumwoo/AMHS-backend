package org.example.amhs.transfer.dto;

import jakarta.validation.constraints.NotBlank;
import org.example.amhs.transfer.domain.TransferPriority;

public record CreateTransferRequestRequest(
        @NotBlank(message = "출발 노드는 필수입니다.")
        String sourceNodeId,
        @NotBlank(message = "도착 노드는 필수입니다.")
        String destinationNodeId,
        TransferPriority priority
) {
}
