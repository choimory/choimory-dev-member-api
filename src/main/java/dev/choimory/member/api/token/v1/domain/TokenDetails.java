package dev.choimory.member.api.token.v1.domain;

import dev.choimory.member.api.member.v1.query.domain.dto.MemberDocumentDto;
import lombok.Builder;

/**
 * 토큰 Claim에 사용할 인증 상세 객체입니다.
 */
@Builder(toBuilder = true)
public record TokenDetails(
        String email // 이메일
) {
    /**
     * 회원 Document DTO에서 토큰 상세 객체를 생성합니다.
     *
     * @param member 회원 Document DTO
     * @return 토큰 상세 객체
     */
    public static TokenDetails of(MemberDocumentDto member) {
        return from(member);
    }

    /**
     * 회원 Document DTO에서 토큰 상세 객체를 생성합니다.
     *
     * @param member 회원 Document DTO
     * @return 토큰 상세 객체
     */
    public static TokenDetails from(MemberDocumentDto member) {
        return TokenDetails.builder()
                .email(member.email())
                .build();
    }
}
