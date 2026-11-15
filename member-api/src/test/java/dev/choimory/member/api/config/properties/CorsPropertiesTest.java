package dev.choimory.member.api.config.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * 환경별 CORS 허용 Origin 설정 테스트입니다. 컨텍스트를 띄우지 않고 환경별 yml을 읽어 바인딩합니다.
 */
class CorsPropertiesTest {

    /**
     * 환경별 yml의 허용 Origin이 계획과 같아야 합니다.
     *
     * @param profile 프로파일
     * @param expected 기대하는 허용 Origin 목록
     * @throws IOException yml 읽기 중 발생
     */
    @ParameterizedTest
    @MethodSource("originsByProfile")
    void allowedOriginsByProfile(String profile, List<String> expected) throws IOException {
        // 환경별 yml을 읽어 cors 프로퍼티로 바인딩한다.
        CorsProperties properties = bind(profile);

        // 허용 Origin이 환경별 계획과 같다.
        assertThat(properties.allowedOrigins()).containsExactlyElementsOf(expected);
    }

    /**
     * 모든 허용 Origin은 스킴을 포함하고 끝에 슬래시가 없어야 브라우저의 Origin과 일치합니다.
     *
     * @throws IOException yml 읽기 중 발생
     */
    @Test
    void allowedOriginsHaveSchemeAndNoTrailingSlash() throws IOException {
        // 모든 환경의 허용 Origin을 모은다.
        List<String> origins = Stream.of("local", "dev", "prod")
                .flatMap(profile -> {
                    try {
                        return bind(profile).allowedOrigins().stream();
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .toList();

        // 스킴이 있고 끝에 슬래시가 없다.
        assertThat(origins).allMatch(origin -> origin.matches("^https?://[^/]+$"));
    }

    private static Stream<Arguments> originsByProfile() {
        return Stream.of(
                Arguments.of("local", List.of("http://localhost:3000")),
                Arguments.of("dev", List.of("http://localhost:3000", "https://dev.choimory.dev")),
                Arguments.of("prod", List.of("https://choimory.dev")));
    }

    private static CorsProperties bind(String profile) throws IOException {
        List<PropertySource<?>> sources =
                new YamlPropertySourceLoader().load(profile, new ClassPathResource("application-" + profile + ".yml"));
        StandardEnvironment environment = new StandardEnvironment();
        sources.forEach(source -> environment.getPropertySources().addLast(source));
        return Binder.get(environment).bind("cors", CorsProperties.class).get();
    }
}
