package dev.choimory.member.api.member.v1.command.domain.response;

import lombok.Builder;

/**
 * 회원 인증 완료 응답 객체입니다.
 */
@Builder(toBuilder = true)
public record VerifyMemberResponse(
        String accessToken, // 액세스 토큰
        String refreshToken // 리프레시 토큰
) {

    /**
     * 회원 인증 완료 응답 객체를 생성합니다.
     *
     * @param accessToken 액세스 토큰
     * @param refreshToken 리프레시 토큰
     * @return 회원 인증 완료 응답 객체
     */
    public static VerifyMemberResponse of(
            String accessToken,
            String refreshToken
    ) {
        return VerifyMemberResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
