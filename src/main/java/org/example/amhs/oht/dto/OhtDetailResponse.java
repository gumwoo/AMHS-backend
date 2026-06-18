package org.example.amhs.oht.dto;

import java.time.OffsetDateTime;
import java.util.List;
import org.example.amhs.oht.domain.Oht;
import org.example.amhs.oht.domain.OhtStatus;

public record OhtDetailResponse(
        String ohtId,
        OhtStatus status,
        String currentNodeId,
        Long currentRequestId,
        String carryingFoupId,
        OffsetDateTime lastMovedAt,
        List<RecentMoveEventResponse> recentMoveEvents
) {

    public static OhtDetailResponse from(Oht oht) {
        return new OhtDetailResponse(
                oht.getOhtId(),
                oht.getStatus(),
                oht.getCurrentNodeId(),
                oht.getCurrentRequestId(),
                oht.getCarryingFoupId(),
                oht.getLastMovedAt(),
                List.of()
        );
    }
}
