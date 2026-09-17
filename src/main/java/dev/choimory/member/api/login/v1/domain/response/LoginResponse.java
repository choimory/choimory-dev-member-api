package dev.choimory.member.api.login.v1.domain.response;

import lombok.Builder;

/**
 * 로그인 응답 객체입니다.
 */
@Builder(toBuilder = true)
public record LoginResponse(
        String accessToken, // 액세스 토큰
        String refreshToken // 리프레시 토큰
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
