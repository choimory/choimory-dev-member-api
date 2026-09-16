package dev.choimory.member.api.member.v1.command.domain.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 회원가입 응답 객체입니다.
 */
@Getter
@Builder(toBuilder = true)
public final class CreateMemberResponse {

    private final String uuid; // 가입 대기 UUID
    private final int verifyCode; // 인증 코드

    /**
     * 회원가입 응답 객체를 생성합니다.
     *
     * @param uuid 가입 대기 UUID
     * @param verifyCode 인증 코드
     */
    @JsonCreator
    public CreateMemberResponse(
            @JsonProperty("uuid") String uuid,
            @JsonProperty("verifyCode") int verifyCode
    ) {
        this.uuid = uuid;
        this.verifyCode = verifyCode;
    }
}
