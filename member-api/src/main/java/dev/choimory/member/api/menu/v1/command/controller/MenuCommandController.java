package dev.choimory.member.api.menu.v1.command.controller;

import dev.choimory.member.api.menu.v1.command.service.MenuCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 메뉴 명령 API 요청을 받는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/v1/menu")
@RequiredArgsConstructor
public class MenuCommandController {

    private final MenuCommandService menuCommandService; // 메뉴 명령 서비스
}
