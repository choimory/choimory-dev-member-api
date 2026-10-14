package dev.choimory.member.api.authority.v1.query.controller;

import dev.choimory.member.api.authority.v1.query.service.AuthorityQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 권한 조회 API 요청을 받는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/v1/authority")
@RequiredArgsConstructor
public class AuthorityQueryController {

    private final AuthorityQueryService authorityQueryService; // 권한 조회 서비스
}
