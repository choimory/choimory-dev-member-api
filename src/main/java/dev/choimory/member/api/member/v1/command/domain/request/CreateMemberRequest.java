package dev.choimory.member.api.member.v1.command.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

/**
 * 회원가입 요청 객체입니다.
 */
@Builder(toBuilder = true)
public record CreateMemberRequest(
        @NotBlank String email, // 이메일
        @NotBlank String password, // 비밀번호
        @NotBlank String nickname, // 닉네임
        String introduce, // 소개글
        MultipartFile profile // 프로필 파일
) {
}
