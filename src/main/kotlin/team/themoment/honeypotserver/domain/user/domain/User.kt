package team.themoment.honeypotserver.domain.user.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import team.themoment.honeypotserver.global.common.BaseTimeEntity

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false, unique = true)
    val oauthAccountId: Long,
    @Column(nullable = false, length = 100)
    var name: String,
    @Column(nullable = false)
    var studentNumber: Int,
    @Column(nullable = true)
    var email: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: Role = Role.GENERAL,
) : BaseTimeEntity() {
    fun syncProfile(
        name: String,
        studentNumber: Int,
        email: String?,
    ) {
        this.name = name
        this.studentNumber = studentNumber
        this.email = email
    }
}
