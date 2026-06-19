package team.themoment.honeypotserver.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfigurationSource
import team.themoment.honeypotserver.global.security.JwtAuthenticationFilter
import team.themoment.honeypotserver.global.security.JwtProvider

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtProvider: JwtProvider,
    private val corsConfigurationSource: CorsConfigurationSource,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .cors { it.configurationSource(corsConfigurationSource) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/v1/auth/datagsm/**",
                        "/v1/auth/reissue",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                    ).permitAll()
                    .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            }
            .addFilterBefore(
                JwtAuthenticationFilter(jwtProvider),
                UsernamePasswordAuthenticationFilter::class.java,
            )

        return http.build()
    }
}
