package team.themoment.honeypotserver.domain.user.presentation

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import team.themoment.honeypotserver.domain.user.application.UserService
import team.themoment.honeypotserver.domain.user.presentation.dto.response.UserResponse
import team.themoment.honeypotserver.global.security.AuthPrincipal
import team.themoment.honeypotserver.global.security.CurrentUser

@RestController
@RequestMapping("/v1/users")
class UserController(
    private val userService: UserService,
) {
    @GetMapping("/me")
    fun getMyProfile(
        @CurrentUser principal: AuthPrincipal,
    ): UserResponse {
        val user = userService.getById(principal.userId)
        return UserResponse.from(user)
    }
}
