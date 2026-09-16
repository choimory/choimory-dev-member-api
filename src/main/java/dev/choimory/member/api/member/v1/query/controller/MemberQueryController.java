package dev.choimory.member.api.member.v1.query.controller;

import dev.choimory.member.api.member.v1.query.service.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 query API Controller입니다.
 */
@RestController
@RequestMapping("/v1/member")
@Validated
@RequiredArgsConstructor
public class MemberQueryController {

    private final MemberQueryService memberQueryService; // 회원 query 서비스
}
