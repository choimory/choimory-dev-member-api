package dev.choimory.member.api.member.v1.command.domain.response;

import lombok.Builder;

/**
 * 회원가입 응답 객체입니다.
 */
@Builder(toBuilder = true)
public record CreateMemberResponse(
        String uuid, // 가입 대기 UUID
        int verifyCode // 인증 코드
        ) {

    /**
     * 회원가입 응답 객체를 생성합니다.
     *
     * @param uuid 가입 대기 UUID
     * @param verifyCode 인증 코드
     * @return 회원가입 응답 객체
     */
    public static CreateMemberResponse of(String uuid, int verifyCode) {
        return CreateMemberResponse.builder().uuid(uuid).verifyCode(verifyCode).build();
    }
}
