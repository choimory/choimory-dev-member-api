package io.choimory.member.external.api.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.choimory.member.external.api.login.v1.security.LoginAuthProvider
import io.choimory.member.external.api.login.v1.security.LoginFailureHandler
import io.choimory.member.external.api.login.v1.security.LoginProcessFilter
import io.choimory.member.external.api.login.v1.security.LoginSuccessHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig(
    val objectMapper: ObjectMapper,
    val loginAuthProvider: LoginAuthProvider,
    val loginSuccessHandler: LoginSuccessHandler,
    val loginFailureHandler: LoginFailureHandler,
) {
    @Bean
    fun config(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
            .cors { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .httpBasic { it.disable() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }

        return http.build()
    }

    @Bean
    fun providerConfig(): AuthenticationManager {
        return ProviderManager(loginAuthProvider)
    }

    @Bean
    fun loginFilterConfig(): LoginProcessFilter {
        return LoginProcessFilter("/login", providerConfig(), loginSuccessHandler, loginFailureHandler, objectMapper)
    }
}
