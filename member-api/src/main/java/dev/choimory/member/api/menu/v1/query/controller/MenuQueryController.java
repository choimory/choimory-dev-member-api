package dev.choimory.member.api.menu.v1.query.controller;

import dev.choimory.member.api.menu.v1.query.service.MenuQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 메뉴 조회 API 요청을 받는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/v1/menu")
@RequiredArgsConstructor
public class MenuQueryController {

    private final MenuQueryService menuQueryService; // 메뉴 조회 서비스
}
