package dev.choimory.member.api.member.v1.command.domain.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

/**
 * 회원가입 요청 객체입니다.
 */
@Getter
@Builder(toBuilder = true)
public final class CreateMemberRequest {

    @NotBlank
    private final String email; // 이메일

    @NotBlank
    private final String password; // 비밀번호

    @NotBlank
    private final String nickname; // 닉네임

    private final String introduce; // 소개글
    private final MultipartFile profile; // 프로필 파일

    /**
     * 회원가입 요청 객체를 생성합니다.
     *
     * @param email 이메일
     * @param password 비밀번호
     * @param nickname 닉네임
     * @param introduce 소개글
     * @param profile 프로필 파일
     */
    @JsonCreator
    public CreateMemberRequest(
            @JsonProperty("email") String email,
            @JsonProperty("password") String password,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("introduce") String introduce,
            @JsonProperty("profile") MultipartFile profile
    ) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.introduce = introduce;
        this.profile = profile;
    }
}
