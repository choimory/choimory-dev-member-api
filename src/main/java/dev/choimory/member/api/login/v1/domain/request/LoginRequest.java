package dev.choimory.member.api.login.v1.domain.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 로그인 요청 객체입니다.
 */
@Getter
@Builder(toBuilder = true)
public final class LoginRequest {

    private final String email; // 이메일
    private final String password; // 비밀번호

    /**
     * 로그인 요청 객체를 생성합니다.
     *
     * @param email 이메일
     * @param password 비밀번호
     */
    @JsonCreator
    public LoginRequest(
            @JsonProperty("email") String email,
            @JsonProperty("password") String password
    ) {
        this.email = email;
        this.password = password;
    }
}
