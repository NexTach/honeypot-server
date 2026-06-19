package team.themoment.honeypotserver.domain.user.presentation.dto.response

import team.themoment.honeypotserver.domain.user.domain.Role
import team.themoment.honeypotserver.domain.user.domain.User

data class UserResponse(
    val id: Long,
    val name: String,
    val studentNumber: Int,
    val email: String?,
    val role: Role,
) {
    companion object {
        fun from(user: User): UserResponse = UserResponse(
            id = user.id,
            name = user.name,
            studentNumber = user.studentNumber,
            email = user.email,
            role = user.role,
        )
    }
}
