package team.themoment.honeypotserver.global.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import team.themoment.honeypotserver.domain.user.domain.Role
import java.nio.charset.StandardCharsets
import java.util.Date

@Component
class JwtProvider(
    private val jwtProperties: JwtProperties,
) {
    private val signingKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray(StandardCharsets.UTF_8))
    }

    fun generateAccessToken(
        userId: Long,
        role: Role,
    ): String {
        val now = System.currentTimeMillis()
        return Jwts
            .builder()
            .subject(userId.toString())
            .claim(CLAIM_ROLE, role.name)
            .issuedAt(Date(now))
            .expiration(Date(now + jwtProperties.accessTtl.toMillis()))
            .signWith(signingKey)
            .compact()
    }

    fun generateRefreshToken(
        userId: Long,
        role: Role,
    ): String {
        val now = System.currentTimeMillis()
        return Jwts
            .builder()
            .subject(userId.toString())
            .claim(CLAIM_ROLE, role.name)
            .issuedAt(Date(now))
            .expiration(Date(now + jwtProperties.refreshTtl.toMillis()))
            .signWith(signingKey)
            .compact()
    }

    fun validate(token: String): Boolean = runCatching { parseClaims(token) }.isSuccess

    fun extractPrincipal(token: String): AuthPrincipal {
        val claims = parseClaims(token)
        val userId = claims.subject.toLong()
        val role = Role.valueOf(claims.get(CLAIM_ROLE, String::class.java))
        return AuthPrincipal(userId = userId, role = role)
    }

    private fun parseClaims(token: String): Claims =
        Jwts
            .parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload

    companion object {
        private const val CLAIM_ROLE = "role"
    }
}
