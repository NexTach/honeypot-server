package team.themoment.honeypotserver.domain.user.application

import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.themoment.honeypotserver.domain.user.domain.User
import team.themoment.honeypotserver.domain.user.infra.DataGsmClient
import team.themoment.honeypotserver.domain.user.infra.UserRepository
import team.themoment.honeypotserver.global.security.JwtProperties
import team.themoment.honeypotserver.global.security.JwtProvider
import team.themoment.sdk.exception.ExpectedException
import java.net.URI
import java.security.SecureRandom
import java.util.Base64

@Service
class AuthService(
    private val dataGsmClient: DataGsmClient,
    private val userRepository: UserRepository,
    private val jwtProvider: JwtProvider,
    private val jwtProperties: JwtProperties,
    @Value("\${honeypot.frontend.redirect-uri}") private val frontendRedirectUri: String,
) {
    private val secureRandom = SecureRandom()

    fun buildLoginRedirect(response: HttpServletResponse): URI {
        val state = generateRandomString(32)
        val codeVerifier = generateRandomString(64)

        setHttpOnlyCookie(response, STATE_COOKIE, state, 300)
        setHttpOnlyCookie(response, CODE_VERIFIER_COOKIE, codeVerifier, 300)

        val authUrl = dataGsmClient.buildLoginUrl(state, codeVerifier)
        return URI.create(authUrl)
    }

    @Transactional
    fun handleCallback(
        code: String,
        state: String,
        cookieState: String?,
        cookieVerifier: String?,
        response: HttpServletResponse,
    ): URI {
        if (cookieState == null || cookieVerifier == null || cookieState != state) {
            throw ExpectedException("Invalid state parameter", HttpStatus.BAD_REQUEST)
        }

        val accessToken = dataGsmClient.exchangeCodeForToken(code, cookieVerifier)
        val userInfo = dataGsmClient.getUserInfo(accessToken)

        if (userInfo.getIsStudent() != true) {
            throw ExpectedException("Only GSM students are allowed", HttpStatus.FORBIDDEN)
        }

        val student = userInfo.student
            ?: throw ExpectedException("Student information not found", HttpStatus.FORBIDDEN)

        val user = userRepository.findByOauthAccountId(userInfo.id)
            ?.also { it.syncProfile(student.name, student.studentNumber, userInfo.email) }
            ?: userRepository.save(
                User(
                    oauthAccountId = userInfo.id,
                    name = student.name,
                    studentNumber = student.studentNumber,
                    email = userInfo.email,
                )
            )

        val jwtAccess = jwtProvider.generateAccessToken(user.id, user.role)
        val jwtRefresh = jwtProvider.generateRefreshToken(user.id, user.role)

        setRefreshTokenCookie(response, jwtRefresh)
        clearOAuthCookies(response)

        return URI.create("$frontendRedirectUri#accessToken=$jwtAccess")
    }

    fun reissueWithCookie(refreshToken: String?, response: HttpServletResponse): String {
        if (refreshToken == null || !jwtProvider.validate(refreshToken)) {
            throw ExpectedException("Invalid or missing refresh token", HttpStatus.UNAUTHORIZED)
        }
        val principal = jwtProvider.extractPrincipal(refreshToken)
        val newAccess = jwtProvider.generateAccessToken(principal.userId, principal.role)
        val newRefresh = jwtProvider.generateRefreshToken(principal.userId, principal.role)

        setRefreshTokenCookie(response, newRefresh)
        return newAccess
    }

    fun logout(response: HttpServletResponse) {
        response.addHeader("Set-Cookie", buildSecureCookie(REFRESH_TOKEN_COOKIE, "", 0))
    }

    private fun setRefreshTokenCookie(response: HttpServletResponse, refreshToken: String) {
        val maxAgeSeconds = jwtProperties.refreshTtl.seconds.toInt()
        val cookie = buildSecureCookie(REFRESH_TOKEN_COOKIE, refreshToken, maxAgeSeconds)
        response.addHeader("Set-Cookie", cookie)
    }

    private fun setHttpOnlyCookie(response: HttpServletResponse, name: String, value: String, maxAge: Int) {
        val cookie = buildSecureCookie(name, value, maxAge)
        response.addHeader("Set-Cookie", cookie)
    }

    private fun buildSecureCookie(name: String, value: String, maxAge: Int): String =
        "$name=$value; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=$maxAge"

    private fun clearOAuthCookies(response: HttpServletResponse) {
        setHttpOnlyCookie(response, STATE_COOKIE, "", 0)
        setHttpOnlyCookie(response, CODE_VERIFIER_COOKIE, "", 0)
    }

    private fun generateRandomString(byteLength: Int): String {
        val bytes = ByteArray(byteLength)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        const val REFRESH_TOKEN_COOKIE = "refreshToken"
        const val STATE_COOKIE = "oauth_state"
        const val CODE_VERIFIER_COOKIE = "oauth_code_verifier"
    }
}
