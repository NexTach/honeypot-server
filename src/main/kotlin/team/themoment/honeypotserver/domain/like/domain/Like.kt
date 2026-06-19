package team.themoment.honeypotserver.domain.like.domain

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import team.themoment.honeypotserver.domain.gif.domain.Gif
import team.themoment.honeypotserver.domain.user.domain.User
import team.themoment.honeypotserver.global.common.BaseTimeEntity

@Entity
@Table(
    name = "likes",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_like_user_gif", columnNames = ["user_id", "gif_id"]),
    ],
)
class Like(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gif_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val gif: Gif,
) : BaseTimeEntity()
