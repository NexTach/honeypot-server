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
        val bearerToken = request.getHeader(AUTHORIZATION_HEADER) ?: return null
        return if (bearerToken.startsWith(BEARER_PREFIX)) {
            bearerToken.removePrefix(BEARER_PREFIX)
        } else {
            null
        }
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }
}
