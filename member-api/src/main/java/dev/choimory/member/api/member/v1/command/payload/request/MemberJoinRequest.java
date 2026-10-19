package dev.choimory.member.api.member.v1.command.payload.request;

import dev.choimory.member.domain.member.command.entity.Member;
import dev.choimory.member.domain.member.command.type.MemberStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * 회원가입 요청 객체입니다.
 *
 * @param email 이메일
 * @param password 비밀번호 (평문, 영문/숫자/특수문자를 모두 포함한 8~64자)
 * @param nickname 닉네임
 * @param introduce 자기소개 (선택)
 */
@Builder(toBuilder = true)
public record MemberJoinRequest(
        @NotBlank @Email @Size(max = 255) String email,

        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$", message = "영문, 숫자, 특수문자를 모두 포함해야 합니다.")
        String password,

        @NotBlank @Size(min = 2, max = 20) String nickname,
        @Size(max = 500) String introduce) {

    /**
     * 회원 엔티티를 생성합니다. 가입 직후 상태는 ACTIVATE입니다.
     *
     * @param encodedPassword 암호화된 비밀번호
     * @return 회원 엔티티
     */
    public Member toEntity(String encodedPassword) {
        // TODO 이메일 인증 도입 시 가입 직후 상태를 REGISTER로 두고 인증 후 ACTIVATE로 올린다.
        return Member.builder()
                .email(email)
                .password(encodedPassword)
                .nickname(nickname)
                .introduce(introduce)
                .status(MemberStatus.ACTIVATE)
                .build();
    }
}
