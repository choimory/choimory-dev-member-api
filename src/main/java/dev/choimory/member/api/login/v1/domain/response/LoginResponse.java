package dev.choimory.member.api.login.v1.domain.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 로그인 응답 객체입니다.
 */
@Getter
@Builder(toBuilder = true)
public final class LoginResponse {

    private final String accessToken; // 액세스 토큰
    private final String refreshToken; // 리프레시 토큰

    /**
     * 로그인 응답 객체를 생성합니다.
     *
     * @param accessToken 액세스 토큰
     * @param refreshToken 리프레시 토큰
     */
    @JsonCreator
    public LoginResponse(
            @JsonProperty("accessToken") String accessToken,
            @JsonProperty("refreshToken") String refreshToken
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
