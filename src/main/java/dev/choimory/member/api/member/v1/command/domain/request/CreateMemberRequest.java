package dev.choimory.member.api.member.v1.command.domain.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

/**
 * 회원가입 요청 객체입니다.
 */
@Builder(toBuilder = true)
public record CreateMemberRequest(
        @JsonProperty("email") @NotBlank String email, // 이메일
        @JsonProperty("password") @NotBlank String password, // 비밀번호
        @JsonProperty("nickname") @NotBlank String nickname, // 닉네임
        @JsonProperty("introduce") String introduce, // 소개글
        @JsonProperty("profile") MultipartFile profile // 프로필 파일
) {
}
