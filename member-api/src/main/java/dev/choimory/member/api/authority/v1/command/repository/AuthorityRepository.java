package dev.choimory.member.api.authority.v1.command.repository;

import dev.choimory.member.domain.authority.command.entity.Authority;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 권한 엔티티의 데이터 접근 인터페이스입니다.
 */
public interface AuthorityRepository extends JpaRepository<Authority, String> {}
