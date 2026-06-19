package team.themoment.honeypotserver.domain.user.infra

import org.springframework.data.jpa.repository.JpaRepository
import team.themoment.honeypotserver.domain.user.domain.User

interface UserRepository : JpaRepository<User, Long> {
    fun findByOauthAccountId(oauthAccountId: Long): User?
}
