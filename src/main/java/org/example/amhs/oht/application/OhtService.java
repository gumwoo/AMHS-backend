package org.example.amhs.oht.application;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.example.amhs.common.exception.ErrorCode;
import org.example.amhs.common.exception.ResourceNotFoundException;
import org.example.amhs.oht.domain.OhtStatus;
import org.example.amhs.oht.dto.OhtDetailResponse;
import org.example.amhs.oht.dto.OhtResponse;
import org.example.amhs.oht.repository.OhtRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OhtService {

    private final OhtRepository ohtRepository;

    public OhtService(OhtRepository ohtRepository) {
        this.ohtRepository = ohtRepository;
    }

    @Transactional(readOnly = true)
    public List<OhtResponse> getOhts(OhtStatus status, String currentNodeId) {
        return ohtRepository.findAll().stream()
                .filter(oht -> status == null || oht.getStatus() == status)
                .filter(oht -> currentNodeId == null || oht.getCurrentNodeId().equals(currentNodeId))
                .sorted(Comparator.comparing(oht -> oht.getOhtId()))
                .map(OhtResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OhtDetailResponse getOht(String ohtId) {
        return ohtRepository.findById(ohtId)
                .map(OhtDetailResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.OHT_NOT_FOUND,
                        Map.of("ohtId", ohtId)
                ));
    }
}
