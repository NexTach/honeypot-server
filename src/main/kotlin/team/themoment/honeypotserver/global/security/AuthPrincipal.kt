package team.themoment.honeypotserver.global.security

import team.themoment.honeypotserver.domain.user.domain.Role

data class AuthPrincipal(
    val userId: Long,
    val role: Role,
)
