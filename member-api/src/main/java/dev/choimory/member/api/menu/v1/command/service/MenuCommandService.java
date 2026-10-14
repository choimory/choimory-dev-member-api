package dev.choimory.member.api.menu.v1.command.service;

import dev.choimory.member.api.menu.v1.command.handler.MenuCommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 메뉴 명령 API의 비즈니스 로직 흐름을 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class MenuCommandService {

    private final MenuCommandHandler menuCommandHandler; // 메뉴 명령 핸들러
}
