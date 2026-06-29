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
                    // 공개 GIF 원본은 외부 메신저(Discord 등)가 인증 없이 링크로 임베드할 수 있어야 한다.
                    // 비공개/블라인드는 GifMediaService 의 가시성 인가가 404 로 차단(앱 계층 관리).
                    .requestMatchers(HttpMethod.GET, "/v1/gifs/*/raw")
                    .permitAll()
                    .requestMatchers("/v1/admin/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated()
            }.addFilterBefore(
                JwtAuthenticationFilter(jwtProvider),
                UsernamePasswordAuthenticationFilter::class.java,
            )

        return http.build()
    }
}
