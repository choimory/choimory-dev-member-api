package dev.choimory.member.api.jwt.v1.command;

import static org.assertj.core.api.Assertions.assertThat;

import dev.choimory.member.api.TestcontainersConfiguration;
import dev.choimory.member.api.jwt.v1.command.repository.JwtRepository;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * JwtRepository 통합 테스트입니다. 실제 Redis(Testcontainers)에 Refresh Token을 저장하고 조회합니다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("local")
class JwtRepositoryTest {

    @Autowired
    private JwtRepository jwtRepository; // JWT 데이터 접근 객체

    @Autowired
    private StringRedisTemplate redisTemplate; // Redis 접근

    /**
     * 저장한 Refresh Token은 존재하고, 삭제하면 사라지며, 다른 jti와 다른 회원과는 구분됩니다.
     */
    @Test
    void saveExistsAndDelete() {
        // 존재하지 않는 토큰은 없어야 한다.
        assertThat(jwtRepository.existsRefreshToken("repo-member-1", "jti-1")).isFalse();

        // 저장하면 해당 회원과 jti의 토큰만 존재해야 한다.
        jwtRepository.saveRefreshToken("repo-member-1", "jti-1", Duration.ofMinutes(10));
        assertThat(jwtRepository.existsRefreshToken("repo-member-1", "jti-1")).isTrue();
        assertThat(jwtRepository.existsRefreshToken("repo-member-1", "jti-2")).isFalse();
        assertThat(jwtRepository.existsRefreshToken("repo-member-2", "jti-1")).isFalse();

        // 삭제하면 사라져야 한다.
        jwtRepository.deleteRefreshToken("repo-member-1", "jti-1");
        assertThat(jwtRepository.existsRefreshToken("repo-member-1", "jti-1")).isFalse();
    }

    /**
     * 저장할 때 TTL이 설정되어 만료 시간이 지나면 Redis에서 사라지는 값이어야 합니다.
     */
    @Test
    void saveSetsTimeToLive() {
        // 14일 TTL로 저장한다.
        jwtRepository.saveRefreshToken("repo-member-3", "jti-ttl", Duration.ofDays(14));

        // 키 형식은 refresh:{memberId}:{jti}이고 TTL이 14일 안팎이어야 한다.
        Long ttl = redisTemplate.getExpire("refresh:repo-member-3:jti-ttl");
        assertThat(ttl)
                .isBetween(
                        Duration.ofDays(14).getSeconds() - 5,
                        Duration.ofDays(14).getSeconds());

        jwtRepository.deleteRefreshToken("repo-member-3", "jti-ttl");
    }
}
