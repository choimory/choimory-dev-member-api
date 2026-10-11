package dev.choimory.member.testsupport;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TestBaseEntity 레포지토리입니다. Persistable 동작(save 시 추가 SELECT 없음)을 검증하는 데 사용합니다.
 */
public interface TestBaseEntityRepository extends JpaRepository<TestBaseEntity, String> {}
