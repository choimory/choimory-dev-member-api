package dev.choimory.member.api.authority.v1.command.repository;

import dev.choimory.member.domain.authority.command.entity.Authority;
import dev.choimory.member.domain.authority.command.type.AuthorityCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 권한 엔티티의 데이터 접근 인터페이스입니다.
 */
public interface AuthorityRepository extends JpaRepository<Authority, String> {

    /**
     * 권한 코드로 권한을 조회합니다.
     *
     * @param code 권한 코드
     * @return 권한 엔티티
     */
    Optional<Authority> findByCode(AuthorityCode code);
}
