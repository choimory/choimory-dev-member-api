package dev.choimory.member.api.menu.v1.query.service;

import dev.choimory.member.api.menu.v1.query.handler.MenuQueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 메뉴 조회 API의 비즈니스 로직 흐름을 담당하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
public class MenuQueryService {

    private final MenuQueryHandler menuQueryHandler; // 메뉴 조회 핸들러
}
