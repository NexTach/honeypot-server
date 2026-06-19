package team.themoment.honeypotserver.domain.user.presentation

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import team.themoment.honeypotserver.domain.user.application.AuthService
import team.themoment.honeypotserver.domain.user.application.AuthService.Companion.CODE_VERIFIER_COOKIE
import team.themoment.honeypotserver.domain.user.application.AuthService.Companion.REFRESH_TOKEN_COOKIE
import team.themoment.honeypotserver.domain.user.application.AuthService.Companion.STATE_COOKIE
import team.themoment.honeypotserver.domain.user.presentation.dto.response.ReissueResponse

@RestController
@RequestMapping("/v1/auth")
class AuthController(
    private val authService: AuthService,
) {

    @GetMapping("/datagsm/login")
    fun login(response: HttpServletResponse): ResponseEntity<Void> {
        val redirectUri = authService.buildLoginRedirect(response)
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, redirectUri.toString())
            .build()
    }

    @GetMapping("/datagsm/callback")
    fun callback(
        @RequestParam code: String,
        @RequestParam state: String,
        @CookieValue(name = STATE_COOKIE, required = false) cookieState: String?,
        @CookieValue(name = CODE_VERIFIER_COOKIE, required = false) cookieVerifier: String?,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        val redirectUri = authService.handleCallback(code, state, cookieState, cookieVerifier, response)
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, redirectUri.toString())
            .build()
    }

    @PostMapping("/reissue")
    fun reissue(
        @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) refreshToken: String?,
        response: HttpServletResponse,
    ): ReissueResponse {
        val accessToken = authService.reissueWithCookie(refreshToken, response)
        return ReissueResponse(accessToken = accessToken)
    }

    @PostMapping("/logout")
    fun logout(response: HttpServletResponse) {
        authService.logout(response)
    }
}
