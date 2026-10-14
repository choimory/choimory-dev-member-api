package dev.choimory.member.domain.common.command.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * CommonResponse 단위 테스트입니다.
 */
class CommonResponseTest {

    /**
     * 데이터가 없는 생성자는 data를 null로 둡니다.
     */
    @Test
    void constructorWithoutDataLeavesDataNull() {
        // 데이터 없이 생성한다.
        CommonResponse<String> response = new CommonResponse<>(404, "NOT_FOUND", "Not Found");

        // 값이 담기고 data는 null이어야 한다.
        assertThat(response.code()).isEqualTo(404);
        assertThat(response.name()).isEqualTo("NOT_FOUND");
        assertThat(response.message()).isEqualTo("Not Found");
        assertThat(response.data()).isNull();
    }

    /**
     * Builder로 생성하면 모든 값이 담깁니다.
     */
    @Test
    void builderSetsAllValues() {
        // Builder로 생성한다.
        CommonResponse<Integer> response = CommonResponse.<Integer>builder()
                .code(200)
                .name("OK")
                .message("OK")
                .data(1)
                .build();

        // 모든 값이 담겨야 한다.
        assertThat(response).isEqualTo(new CommonResponse<>(200, "OK", "OK", 1));
    }

    /**
     * toBuilder로 일부 값만 바꿔 복사할 수 있습니다.
     */
    @Test
    void toBuilderCopiesWithChanges() {
        // 원본에서 data만 바꿔 복사한다.
        CommonResponse<String> origin = new CommonResponse<>(200, "OK", "OK", "a");
        CommonResponse<String> copy = origin.toBuilder().data("b").build();

        // data만 바뀌고 나머지는 같아야 한다.
        assertThat(copy.data()).isEqualTo("b");
        assertThat(copy.code()).isEqualTo(origin.code());
        assertThat(origin.data()).isEqualTo("a");
    }
}
