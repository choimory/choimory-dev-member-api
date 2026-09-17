package dev.choimory.member.api.login.v1.domain.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * 로그인 응답 객체입니다.
 */
@Builder(toBuilder = true)
public record LoginResponse(
        @JsonProperty("accessToken") String accessToken, // 액세스 토큰
        @JsonProperty("refreshToken") String refreshToken // 리프레시 토큰
) {

    /**
     * 로그인 응답 객체를 생성합니다.
     *
     * @param accessToken 액세스 토큰
     * @param refreshToken 리프레시 토큰
     * @return 로그인 응답 객체
     */
    public static LoginResponse of(
            String accessToken,
            String refreshToken
    ) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
