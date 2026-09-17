package dev.choimory.member.api.member.v1.command.repository;

import dev.choimory.member.api.member.v1.command.domain.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 회원 command RDB 접근 Repository입니다.
 */
@Repository
public interface MemberCommandRepository extends JpaRepository<MemberEntity, String> {}
