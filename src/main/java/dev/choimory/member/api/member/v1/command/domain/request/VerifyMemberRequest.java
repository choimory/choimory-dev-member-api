package dev.choimory.member.api.member.v1.command.domain.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 회원 인증 요청 객체입니다.
 */
@Getter
@Builder(toBuilder = true)
public final class VerifyMemberRequest {

    private final String email; // 이메일
    private final String uuid; // 가입 대기 UUID
    private final int verifyCode; // 인증 코드

    /**
     * 회원 인증 요청 객체를 생성합니다.
     *
     * @param email 이메일
     * @param uuid 가입 대기 UUID
     * @param verifyCode 인증 코드
     */
    @JsonCreator
    public VerifyMemberRequest(
            @JsonProperty("email") String email,
            @JsonProperty("uuid") String uuid,
            @JsonProperty("verifyCode") int verifyCode
    ) {
        this.email = email;
        this.uuid = uuid;
        this.verifyCode = verifyCode;
    }
}
