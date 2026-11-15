package dev.choimory.member.api.menu.v1.command.handler;

import dev.choimory.member.api.menu.v1.command.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 메뉴 명령의 세부 로직을 담당하는 핸들러입니다.
 */
@Component
@RequiredArgsConstructor
public class MenuCommandHandler {

    private final MenuRepository menuRepository; // 메뉴 데이터 접근 객체
}
