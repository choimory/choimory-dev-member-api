package dev.choimory.member.api.member.v1.command.domain.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.choimory.member.api.member.v1.command.domain.entity.MemberEntity;
import lombok.Builder;
import lombok.Getter;

/**
 * 회원 Entity와 1:1로 매칭되는 DTO입니다.
 */
@Getter
@Builder(toBuilder = true)
public final class MemberEntityDto {

    private final String id; // 회원 ID
    private final String email; // 이메일
    private final String nickname; // 닉네임
    private final String password; // 암호화된 비밀번호
    private final String introduce; // 소개글

    /**
     * 회원 DTO를 생성합니다.
     *
     * @param id 회원 ID
     * @param email 이메일
     * @param nickname 닉네임
     * @param password 암호화된 비밀번호
     * @param introduce 소개글
     */
    @JsonCreator
    public MemberEntityDto(
            @JsonProperty("id") String id,
            @JsonProperty("email") String email,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("password") String password,
            @JsonProperty("introduce") String introduce
    ) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.introduce = introduce;
    }

    /**
     * 회원 DTO를 회원 Entity로 변환합니다.
     *
     * @param dto 회원 DTO
     * @return 회원 Entity
     */
    public static MemberEntity toEntity(MemberEntityDto dto) {
        return MemberEntity.create(
                dto.getId(),
                dto.getEmail(),
                dto.getNickname(),
                dto.getPassword(),
                dto.getIntroduce()
        );
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
