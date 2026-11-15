package dev.choimory.member.api.member.v1.command.repository;

import dev.choimory.member.domain.member.command.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 회원 엔티티의 데이터 접근 인터페이스입니다.
 */
public interface MemberRepository extends JpaRepository<Member, String> {

    /**
     * 이메일의 사용 여부를 soft delete된 회원까지 포함해 확인합니다.
     * 엔티티 쿼리는 {@code @SQLRestriction}으로 삭제된 행을 제외하므로 네이티브 쿼리를 사용합니다.
     *
     * @param email 이메일
     * @return 삭제된 회원을 포함해 사용 중이면 true
     */
    @Query(value = "select exists(select 1 from member where email = :email)", nativeQuery = true)
    boolean existsByEmailIncludingDeleted(@Param("email") String email);

    /**
     * 닉네임의 사용 여부를 soft delete된 회원까지 포함해 확인합니다.
     * 엔티티 쿼리는 {@code @SQLRestriction}으로 삭제된 행을 제외하므로 네이티브 쿼리를 사용합니다.
     *
     * @param nickname 닉네임
     * @return 삭제된 회원을 포함해 사용 중이면 true
     */
    @Query(value = "select exists(select 1 from member where nickname = :nickname)", nativeQuery = true)
    boolean existsByNicknameIncludingDeleted(@Param("nickname") String nickname);

    /**
     * 이메일로 회원을 조회합니다. 로그인에 필요한 권한을 함께 가져오며, soft delete된 회원은 제외됩니다.
     *
     * @param email 이메일
     * @return 권한을 포함한 회원 엔티티
     */
    @EntityGraph(attributePaths = {"authorities", "authorities.authority"})
    Optional<Member> findByEmail(String email);

    /**
     * ID로 회원을 조회합니다. 토큰 재발급에 필요한 권한을 함께 가져오며, soft delete된 회원은 제외됩니다.
     *
     * @param id 회원 ID
     * @return 권한을 포함한 회원 엔티티
     */
    @EntityGraph(attributePaths = {"authorities", "authorities.authority"})
    Optional<Member> findWithAuthoritiesById(String id);
}
