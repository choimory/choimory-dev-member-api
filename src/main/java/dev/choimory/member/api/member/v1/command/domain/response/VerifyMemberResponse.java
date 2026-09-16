package dev.choimory.member.api.member.v1.command.domain.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 회원 인증 완료 응답 객체입니다.
 */
@Getter
@Builder(toBuilder = true)
public final class VerifyMemberResponse {

    private final String accessToken; // 액세스 토큰
    private final String refreshToken; // 리프레시 토큰

    /**
     * 회원 인증 완료 응답 객체를 생성합니다.
     *
     * @param accessToken 액세스 토큰
     * @param refreshToken 리프레시 토큰
     */
    @JsonCreator
    public VerifyMemberResponse(
            @JsonProperty("accessToken") String accessToken,
            @JsonProperty("refreshToken") String refreshToken
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
