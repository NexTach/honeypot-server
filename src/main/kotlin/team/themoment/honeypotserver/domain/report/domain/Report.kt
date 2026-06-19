package team.themoment.honeypotserver.domain.report.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import team.themoment.honeypotserver.domain.gif.domain.Gif
import team.themoment.honeypotserver.domain.user.domain.User
import team.themoment.honeypotserver.global.common.BaseTimeEntity
import java.time.Instant

@Entity
@Table(
    name = "reports",
    indexes = [
        Index(name = "idx_report_status", columnList = "status"),
    ],
)
class Report(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    val reporter: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gif_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    val gif: Gif,

    @Column(nullable = false, length = 100)
    val reasonTitle: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val detail: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ReportStatus = ReportStatus.PENDING,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_id", nullable = true)
    var processedBy: User? = null,

    @Column(nullable = true)
    var processedAt: Instant? = null,
) : BaseTimeEntity()
