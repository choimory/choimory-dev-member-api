package dev.choimory.member.api.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisTemplate 설정입니다.
 */
@Configuration
public class RedisConfig {

    /**
     * 문자열 Key와 JSON Value를 사용하는 RedisTemplate을 생성합니다.
     *
     * @param connectionFactory Redis connection factory
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory, GenericJackson2JsonRedisSerializer redisValueSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(redisValueSerializer);
        return template;
    }

    /**
     * Java record 타입 정보를 포함하는 Redis JSON Value Serializer를 생성합니다.
     *
     * @return Redis JSON Value Serializer
     */
    @Bean
    public GenericJackson2JsonRedisSerializer redisValueSerializer() {
        return new GenericJackson2JsonRedisSerializer(redisObjectMapper());
    }

    /**
     * Redis JSON 직렬화용 ObjectMapper를 생성합니다.
     *
     * @return Redis JSON 직렬화용 ObjectMapper
     */
    private ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        PolymorphicTypeValidator polymorphicTypeValidator = objectMapper.getPolymorphicTypeValidator();
        RecordSupportingTypeResolver typeResolver =
                new RecordSupportingTypeResolver(ObjectMapper.DefaultTyping.NON_FINAL, polymorphicTypeValidator);
        typeResolver.init(JsonTypeInfo.Id.CLASS, null);
        typeResolver.inclusion(JsonTypeInfo.As.PROPERTY);
        objectMapper.setDefaultTyping(typeResolver);

        return objectMapper;
    }

    /**
     * Java record에도 타입 정보를 포함시키는 Jackson TypeResolver입니다.
     */
    private static final class RecordSupportingTypeResolver extends ObjectMapper.DefaultTypeResolverBuilder {

        /**
         * Java record 지원 TypeResolver를 생성합니다.
         *
         * @param defaultTyping 기본 타입 정보 적용 정책
         * @param polymorphicTypeValidator 다형성 타입 검증 객체
         */
        private RecordSupportingTypeResolver(
                ObjectMapper.DefaultTyping defaultTyping, PolymorphicTypeValidator polymorphicTypeValidator) {
            super(defaultTyping, polymorphicTypeValidator);
        }

        /**
         * 타입 정보 적용 여부를 확인합니다.
         *
         * @param type 확인할 Java 타입
         * @return 타입 정보 적용 여부
         */
        @Override
        public boolean useForType(JavaType type) {
            if (type.getRawClass().isRecord()) {
                return true;
            }
            return super.useForType(type);
        }
    }
}
