package dev.choimory.member.api.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * CommonException 단위 테스트입니다.
 */
class CommonExceptionTest {

    /**
     * HTTP 상태만 지정하면 상태의 기본값으로 응답 값이 채워집니다.
     */
    @Test
    void statusOnlyFillsDefaults() {
        // HTTP 상태만 지정해 생성한다.
        CommonException exception = new CommonException(HttpStatus.NOT_FOUND);

        // 상태의 기본값으로 채워져야 한다.
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getCode()).isEqualTo(404);
        assertThat(exception.getCodeName()).isEqualTo("NOT_FOUND");
        assertThat(exception.getMsg()).isEqualTo("Not Found");
        assertThat(exception.getMessage()).isEqualTo("Not Found");
    }

    /**
     * 세부 응답 값을 지정하면 그대로 담깁니다.
     */
    @Test
    void customValuesAreKept() {
        // 세부 값을 지정해 생성한다.
        CommonException exception = new CommonException(HttpStatus.CONFLICT, 1001, "DUPLICATE_EMAIL", "중복 이메일");

        // 지정한 값이 담겨야 한다.
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getCode()).isEqualTo(1001);
        assertThat(exception.getCodeName()).isEqualTo("DUPLICATE_EMAIL");
        assertThat(exception.getMsg()).isEqualTo("중복 이메일");
        assertThat(exception.getMessage()).isEqualTo("중복 이메일");
    }
}
