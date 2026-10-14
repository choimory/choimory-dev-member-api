package dev.choimory.member.api.member.v1.command.repository;

import dev.choimory.member.domain.member.command.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 회원 엔티티의 데이터 접근 인터페이스입니다.
 */
public interface MemberRepository extends JpaRepository<Member, String> {}
