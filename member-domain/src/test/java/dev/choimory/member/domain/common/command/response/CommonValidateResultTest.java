package dev.choimory.member.domain.common.command.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * CommonValidateResult 단위 테스트입니다.
 */
class CommonValidateResultTest {

    /**
     * Builder로 생성하면 모든 값이 담깁니다.
     */
    @Test
    void builderSetsAllValues() {
        // Builder로 생성한다.
        CommonValidateResult result = CommonValidateResult.builder()
                .field("email")
                .rejectValue("abc")
                .message("must be a well-formed email address")
                .build();

        // 모든 값이 담겨야 한다.
        assertThat(result.field()).isEqualTo("email");
        assertThat(result.rejectValue()).isEqualTo("abc");
        assertThat(result.message()).isEqualTo("must be a well-formed email address");
    }

    /**
     * toBuilder로 일부 값만 바꿔 복사할 수 있습니다.
     */
    @Test
    void toBuilderCopiesWithChanges() {
        // 원본에서 message만 바꿔 복사한다.
        CommonValidateResult origin = new CommonValidateResult("email", "abc", "a");
        CommonValidateResult copy = origin.toBuilder().message("b").build();

        // message만 바뀌어야 한다.
        assertThat(copy).isEqualTo(new CommonValidateResult("email", "abc", "b"));
    }
}
