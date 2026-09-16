package dev.choimory.member.api.member.v1.command.controller;

import dev.choimory.member.api.common.domain.response.CommonResponse;
import dev.choimory.member.api.member.v1.command.domain.request.CreateMemberRequest;
import dev.choimory.member.api.member.v1.command.domain.request.VerifyMemberRequest;
import dev.choimory.member.api.member.v1.command.domain.response.CreateMemberResponse;
import dev.choimory.member.api.member.v1.command.domain.response.VerifyMemberResponse;
import dev.choimory.member.api.member.v1.command.service.MemberCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 command API Controller입니다.
 */
@RestController
@RequestMapping("/v1/member")
@Validated
@RequiredArgsConstructor
public class MemberCommandController {

    private final MemberCommandService memberCommandService; // 회원 command 서비스

    /**
     * 회원가입 요청을 처리합니다.
     *
     * @param payload 회원가입 요청
     * @return 회원가입 인증 정보 응답
     */
    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.OK)
    public CommonResponse<CreateMemberResponse> signUp(@Valid @ModelAttribute CreateMemberRequest payload) {
        return memberCommandService.signUp(payload);
    }

    /**
     * 회원가입 인증 요청을 처리합니다.
     *
     * @param payload 회원 인증 요청
     * @return 회원 인증 완료 응답
     */
    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<VerifyMemberResponse> verify(@Valid @RequestBody VerifyMemberRequest payload) {
        return memberCommandService.verify(payload);
    }
}
