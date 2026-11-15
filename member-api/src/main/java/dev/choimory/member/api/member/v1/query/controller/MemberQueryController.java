package dev.choimory.member.api.member.v1.query.controller;

import dev.choimory.member.api.member.v1.query.service.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 조회 API 요청을 받는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/v1/member")
@RequiredArgsConstructor
public class MemberQueryController {

    private final MemberQueryService memberQueryService; // 회원 조회 서비스
}
