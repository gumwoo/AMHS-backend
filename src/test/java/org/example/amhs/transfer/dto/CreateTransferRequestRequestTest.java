package org.example.amhs.transfer.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.example.amhs.transfer.domain.TransferPriority;
import org.junit.jupiter.api.Test;

class CreateTransferRequestRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 출발지와_도착지는_필수값이다() {
        var violations = validator.validate(new CreateTransferRequestRequest(
                "",
                null,
                TransferPriority.NORMAL
        ));

        assertThat(violations)
                .extracting(violation -> violation.getMessage())
                .containsExactlyInAnyOrder("출발 노드는 필수입니다.", "도착 노드는 필수입니다.");
    }
}
