package dev.choimory.member.api.agreement.v1.command.repository;

import dev.choimory.member.domain.agreement.command.entity.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 약관 엔티티의 데이터 접근 인터페이스입니다.
 */
public interface AgreementRepository extends JpaRepository<Agreement, String> {}
