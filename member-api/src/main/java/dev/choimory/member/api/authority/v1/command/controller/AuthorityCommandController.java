package dev.choimory.member.api.authority.v1.command.controller;

import dev.choimory.member.api.authority.v1.command.service.AuthorityCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 권한 명령 API 요청을 받는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/v1/authority")
@RequiredArgsConstructor
public class AuthorityCommandController {

    private final AuthorityCommandService authorityCommandService; // 권한 명령 서비스
}
