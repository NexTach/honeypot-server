package team.themoment.honeypotserver.global.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = resolveToken(request)

        if (token != null && jwtProvider.validate(token)) {
            val principal = jwtProvider.extractPrincipal(token)
            val authorities = listOf(SimpleGrantedAuthority("ROLE_${principal.role.name}"))
            val authentication = UsernamePasswordAuthenticationToken(principal, null, authorities)
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader(AUTHORIZATION_HEADER)
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.removePrefix(BEARER_PREFIX)
        }

        // 미디어 서빙(`/raw`)은 <img>·next/image 가 직접 호출하므로 Authorization 헤더를 실을 수 없다.
        // same-origin 으로 자동 전송되는 accessToken 쿠키를 이 경로에 한해 fallback 으로 허용한다.
        if (isMediaRequest(request)) {
            return request.cookies?.firstOrNull { it.name == ACCESS_TOKEN_COOKIE }?.value
        }

        return null
    }

    private fun isMediaRequest(request: HttpServletRequest): Boolean = RAW_PATH_REGEX.matches(request.requestURI)

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private const val ACCESS_TOKEN_COOKIE = "accessToken"
        private val RAW_PATH_REGEX = Regex("""^/v1/gifs/\d+/raw(\.gif)?$""")
    }
}
