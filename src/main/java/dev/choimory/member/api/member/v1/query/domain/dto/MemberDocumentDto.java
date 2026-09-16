package dev.choimory.member.api.member.v1.query.domain.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.choimory.member.api.member.v1.query.domain.document.MemberDocument;
import lombok.Builder;
import lombok.Getter;

/**
 * 회원 Document와 매칭되는 DTO입니다.
 */
@Getter
@Builder(toBuilder = true)
public final class MemberDocumentDto {

    private final String email; // 이메일

    /**
     * 회원 Document DTO를 생성합니다.
     *
     * @param email 이메일
     */
    @JsonCreator
    public MemberDocumentDto(@JsonProperty("email") String email) {
        this.email = email;
    }

    /**
     * 회원 Document를 DTO로 변환합니다.
     *
     * @param memberDocument 회원 Document
     * @return 회원 Document DTO
     */
    public static MemberDocumentDto from(MemberDocument memberDocument) {
        return MemberDocumentDto.builder()
                .email(memberDocument.getEmail())
                .build();
    }
}
