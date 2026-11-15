package dev.choimory.member.api.member.v1.command.controller;

import dev.choimory.member.api.member.v1.command.payload.request.MemberJoinRequest;
import dev.choimory.member.api.member.v1.command.payload.request.MemberReissueRequest;
import dev.choimory.member.api.member.v1.command.payload.response.MemberJoinResponse;
import dev.choimory.member.api.member.v1.command.payload.response.MemberTokenResponse;
import dev.choimory.member.api.member.v1.command.service.MemberCommandService;
import dev.choimory.member.domain.common.command.response.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 명령 API 요청을 받는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/v1/member")
@RequiredArgsConstructor
public class MemberCommandController {

    private final MemberCommandService memberCommandService; // 회원 명령 서비스

    /**
     * 회원가입합니다.
     *
     * @param request 회원가입 요청
     * @return 회원가입 응답
     */
    @PostMapping("/join")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<MemberJoinResponse> join(@Valid @RequestBody final MemberJoinRequest request) {
        return new CommonResponse<>(
                HttpStatus.CREATED.value(),
                HttpStatus.CREATED.name(),
                HttpStatus.CREATED.getReasonPhrase(),
                memberCommandService.join(request));
    }

    /**
     * Refresh Token으로 Access Token과 Refresh Token을 다시 발급합니다.
     *
     * @param request 재발급 요청
     * @return 새로 발급된 토큰
     */
    @PostMapping("/reissue")
    public CommonResponse<MemberTokenResponse> reissue(@Valid @RequestBody final MemberReissueRequest request) {
        return new CommonResponse<>(
                HttpStatus.OK.value(),
                HttpStatus.OK.name(),
                HttpStatus.OK.getReasonPhrase(),
                memberCommandService.reissue(request));
    }
}
