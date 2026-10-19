package dev.choimory.member.api.jwt.v1.command.repository;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * JWT 관련 데이터의 Redis 접근 클래스입니다.
 * Refresh Token은 {@code refresh:{memberId}:{jti}} 키로 저장하며, 토큰 자체는 서명과 jti로 검증하므로 값은 표식만 둡니다.
 * 로그인할 때마다 새 jti로 저장되므로 기기별 다중 로그인이 됩니다.
 */
@Repository
@RequiredArgsConstructor
public class JwtRepository {

    private static final String REFRESH_KEY_PREFIX = "refresh:"; // Refresh Token 키 접두어
    private static final String MARKER = "1"; // 값 표식

    private final StringRedisTemplate redisTemplate; // Redis 접근

    /**
     * Refresh Token을 저장합니다. 만료 시간이 지나면 Redis에서 자동으로 사라집니다.
     *
     * @param memberId 회원 ID
     * @param jti 토큰 고유 ID
     * @param ttl 토큰 만료까지 남은 시간
     */
    public void saveRefreshToken(String memberId, String jti, Duration ttl) {
        redisTemplate.opsForValue().set(refreshKey(memberId, jti), MARKER, ttl);
    }

    /**
     * 저장된 Refresh Token인지 확인합니다.
     *
     * @param memberId 회원 ID
     * @param jti 토큰 고유 ID
     * @return 저장되어 있으면 true
     */
    public boolean existsRefreshToken(String memberId, String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(refreshKey(memberId, jti)));
    }

    /**
     * Refresh Token을 삭제합니다. 동시에 같은 토큰을 쓰려는 요청 중 하나만 삭제에 성공합니다.
     *
     * @param memberId 회원 ID
     * @param jti 토큰 고유 ID
     * @return 저장되어 있던 토큰을 삭제했으면 true, 이미 없었으면 false
     */
    public boolean deleteRefreshToken(String memberId, String jti) {
        return Boolean.TRUE.equals(redisTemplate.delete(refreshKey(memberId, jti)));
    }

    /**
     * Refresh Token의 Redis 키를 만듭니다.
     *
     * @param memberId 회원 ID
     * @param jti 토큰 고유 ID
     * @return Redis 키
     */
    private String refreshKey(String memberId, String jti) {
        return REFRESH_KEY_PREFIX + memberId + ":" + jti;
    }
}
