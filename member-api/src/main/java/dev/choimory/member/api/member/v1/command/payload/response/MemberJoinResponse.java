package dev.choimory.member.api.member.v1.command.payload.response;

import dev.choimory.member.domain.member.command.dto.MemberDto;
import dev.choimory.member.domain.member.command.type.MemberStatus;
import lombok.Builder;

/**
 * 회원가입 응답 객체입니다. 비밀번호는 포함하지 않습니다.
 *
 * @param id 회원 ID
 * @param email 이메일
 * @param nickname 닉네임
 * @param status 회원 상태
 */
@Builder(toBuilder = true)
public record MemberJoinResponse(String id, String email, String nickname, MemberStatus status) {

    /**
     * 회원 DTO를 회원가입 응답으로 변환합니다.
     *
     * @param member 회원 DTO
     * @return 회원가입 응답
     */
    public static MemberJoinResponse from(MemberDto member) {
        return MemberJoinResponse.builder()
                .id(member.id())
                .email(member.email())
                .nickname(member.nickname())
                .status(member.status())
                .build();
    }
}
