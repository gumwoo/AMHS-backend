package org.example.amhs.oht.dto;

import java.time.OffsetDateTime;
import org.example.amhs.oht.domain.Oht;
import org.example.amhs.oht.domain.OhtStatus;

public record OhtResponse(
        String ohtId,
        OhtStatus status,
        String currentNodeId,
        Long currentRequestId,
        String carryingFoupId,
        OffsetDateTime lastMovedAt
) {

    public static OhtResponse from(Oht oht) {
        return new OhtResponse(
                oht.getOhtId(),
                oht.getStatus(),
                oht.getCurrentNodeId(),
                oht.getCurrentRequestId(),
                oht.getCarryingFoupId(),
                oht.getLastMovedAt()
        );
    }
}
