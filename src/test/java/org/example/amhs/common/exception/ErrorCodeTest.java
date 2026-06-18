package org.example.amhs.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorCodeTest {

    @Test
    void 에러_메시지는_깨지지_않은_한국어로_제공된다() {
        assertThat(ErrorCode.VALIDATION_ERROR.message()).isEqualTo("요청 값 검증에 실패했습니다.");
        assertThat(ErrorCode.TRANSFER_REQUEST_NOT_FOUND.message()).isEqualTo("반송 요청을 찾을 수 없습니다.");
        assertThat(ErrorCode.SIMULATION_ALREADY_RUNNING.message()).isEqualTo("시뮬레이션이 이미 실행 중입니다.");
    }
}
