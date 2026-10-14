package dev.choimory.member.api.member.v1.command.controller;

import dev.choimory.member.api.member.v1.command.service.MemberCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 명령 API 요청을 받는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/v1/member")
@RequiredArgsConstructor
public class MemberCommandController {

    private final MemberCommandService memberCommandService; // 회원 명령 서비스
}
