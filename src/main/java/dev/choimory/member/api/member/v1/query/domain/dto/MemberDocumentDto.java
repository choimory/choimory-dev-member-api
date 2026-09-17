package dev.choimory.member.api.member.v1.query.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.choimory.member.api.common.domain.document.CommonDateDocument;
import dev.choimory.member.api.member.v1.query.domain.document.MemberDocument;
import lombok.Builder;

/**
 * 회원 Document와 매칭되는 DTO입니다.
 */
@Builder(toBuilder = true)
public record MemberDocumentDto(
        @JsonProperty("id") String id, // 회원 ID
        @JsonProperty("email") String email, // 이메일
        @JsonProperty("nickname") String nickname, // 닉네임
        @JsonProperty("password") String password, // 암호화된 비밀번호
        @JsonProperty("introduce") String introduce, // 소개글
        @JsonProperty("commonDate") CommonDateDocument commonDate // 공통 일시 정보
) {

    /**
     * 회원 Document를 DTO로 변환합니다.
     *
     * @param memberDocument 회원 Document
     * @return 회원 Document DTO
     */
    public static MemberDocumentDto from(MemberDocument memberDocument) {
        return MemberDocumentDto.builder()
                .id(memberDocument.getId())
                .email(memberDocument.getEmail())
                .nickname(memberDocument.getNickname())
                .password(memberDocument.getPassword())
                .introduce(memberDocument.getIntroduce())
                .commonDate(memberDocument.getCommonDate())
                .build();
    }
}
