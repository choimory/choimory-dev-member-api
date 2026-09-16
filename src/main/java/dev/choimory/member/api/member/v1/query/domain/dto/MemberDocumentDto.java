package dev.choimory.member.api.member.v1.query.domain.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.choimory.member.api.common.domain.document.CommonDateDocument;
import dev.choimory.member.api.member.v1.query.domain.document.MemberDocument;
import lombok.Builder;
import lombok.Getter;

/**
 * 회원 Document와 매칭되는 DTO입니다.
 */
@Getter
@Builder(toBuilder = true)
public final class MemberDocumentDto {

    private final String id; // 회원 ID
    private final String email; // 이메일
    private final String nickname; // 닉네임
    private final String password; // 암호화된 비밀번호
    private final String introduce; // 소개글
    private final CommonDateDocument commonDate; // 공통 일시 정보

    /**
     * 회원 Document DTO를 생성합니다.
     *
     * @param id 회원 ID
     * @param email 이메일
     * @param nickname 닉네임
     * @param password 암호화된 비밀번호
     * @param introduce 소개글
     * @param commonDate 공통 일시 정보
     */
    @JsonCreator
    public MemberDocumentDto(
            @JsonProperty("id") String id,
            @JsonProperty("email") String email,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("password") String password,
            @JsonProperty("introduce") String introduce,
            @JsonProperty("commonDate") CommonDateDocument commonDate
    ) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.introduce = introduce;
        this.commonDate = commonDate;
    }

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
