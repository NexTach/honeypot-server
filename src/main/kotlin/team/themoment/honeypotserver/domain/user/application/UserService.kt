package team.themoment.honeypotserver.domain.user.application

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.themoment.honeypotserver.domain.user.domain.User
import team.themoment.honeypotserver.domain.user.infra.UserRepository
import team.themoment.sdk.exception.ExpectedException

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
) {

    fun getById(userId: Long): User =
        userRepository.findById(userId).orElseThrow {
            ExpectedException("User not found", HttpStatus.NOT_FOUND)
        }
}
