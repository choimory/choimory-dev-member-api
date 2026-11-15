package dev.choimory.member.api.menu.v1.command.repository;

import dev.choimory.member.domain.authority.command.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 메뉴 엔티티의 데이터 접근 인터페이스입니다.
 */
public interface MenuRepository extends JpaRepository<Menu, String> {}
