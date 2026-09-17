package dev.choimory.member.api.member.v1.command.domain.dto;

import dev.choimory.member.api.member.v1.command.domain.entity.MemberEntity;
import lombok.Builder;

/**
 * 회원 Entity와 1:1로 매칭되는 DTO입니다.
 */
@Builder(toBuilder = true)
public record MemberEntityDto(
        String id, // 회원 ID
        String email, // 이메일
        String nickname, // 닉네임
        String password, // 암호화된 비밀번호
        String introduce // 소개글
        ) {

    /**
     * 회원 DTO를 회원 Entity로 변환합니다.
     *
     * @param dto 회원 DTO
     * @return 회원 Entity
     */
    public static MemberEntity toEntity(MemberEntityDto dto) {
        return MemberEntity.create(dto.id(), dto.email(), dto.nickname(), dto.password(), dto.introduce());
    }

    /**
     * 회원 Entity를 회원 DTO로 변환합니다.
     *
     * @param entity 회원 Entity
     * @return 회원 DTO
     */
    public static MemberEntityDto from(MemberEntity entity) {
        return MemberEntityDto.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .nickname(entity.getNickname())
                .password(entity.getPassword())
                .introduce(entity.getIntroduce())
                .build();
    }
}
