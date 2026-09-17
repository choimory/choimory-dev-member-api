package dev.choimory.member.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.choimory.member.api.member.v1.command.domain.dto.MemberEntityDto;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

/**
 * Redis 설정 테스트입니다.
 */
class RedisConfigTest {

    /**
     * Redis JSON Value Serializer가 record 타입 정보를 포함하고 record 객체로 복원하는지 확인합니다.
     */
    @Test
    void redisValueSerializerDeserializeRecord() {
        // Redis JSON Value Serializer를 생성한다.
        GenericJackson2JsonRedisSerializer serializer = new RedisConfig().redisValueSerializer();

        // Redis에 저장할 회원 Entity DTO record를 생성한다.
        MemberEntityDto member = MemberEntityDto.builder()
                .id("member-id")
                .email("member@example.com")
                .nickname("nickname")
                .password("encoded-password")
                .introduce("introduce")
                .build();

        // record 객체를 JSON byte array로 직렬화한다.
        byte[] serialized = serializer.serialize(member);
        String json = new String(serialized, StandardCharsets.UTF_8);

        // record 객체에 타입 정보가 포함되었는지 확인한다.
        assertThat(json).contains("\"@class\"").contains(MemberEntityDto.class.getName());

        // JSON byte array를 다시 객체로 역직렬화한다.
        Object deserialized = serializer.deserialize(serialized);

        // 역직렬화 결과가 기존 record 타입과 값으로 복원되었는지 확인한다.
        assertThat(deserialized).isInstanceOf(MemberEntityDto.class).isEqualTo(member);
    }
}
