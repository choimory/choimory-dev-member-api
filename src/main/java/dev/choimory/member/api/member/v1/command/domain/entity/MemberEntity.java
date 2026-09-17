package dev.choimory.member.api.member.v1.command.domain.entity;

import dev.choimory.member.api.common.domain.entity.CommonDateEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 RDB Entity입니다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberEntity extends CommonDateEntity {

    @Id
    private String id; // 회원 ID

    private String email; // 이메일
    private String nickname; // 닉네임
    private String password; // 암호화된 비밀번호
    private String introduce; // 소개글

    /**
     * 회원 Entity를 생성합니다.
     *
     * @param id 회원 ID
     * @param email 이메일
     * @param nickname 닉네임
     * @param password 암호화된 비밀번호
     * @param introduce 소개글
     */
    private MemberEntity(String id, String email, String nickname, String password, String introduce) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.introduce = introduce;
    }

    /**
     * 회원 Entity를 생성합니다.
     *
     * @param id 회원 ID
     * @param email 이메일
     * @param nickname 닉네임
     * @param password 암호화된 비밀번호
     * @param introduce 소개글
     * @return 회원 Entity
     */
    public static MemberEntity create(String id, String email, String nickname, String password, String introduce) {
        return new MemberEntity(id, email, nickname, password, introduce);
    }
}
