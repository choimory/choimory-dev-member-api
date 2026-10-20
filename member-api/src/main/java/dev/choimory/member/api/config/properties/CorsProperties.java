package dev.choimory.member.api.config.properties;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * CORS 설정값입니다. (application-{profile}.yml)
 *
 * @param allowedOrigins 허용할 Origin 목록 (스킴과 포트를 포함한 프론트 주소)
 */
@Validated
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(@NotEmpty List<String> allowedOrigins) {}
