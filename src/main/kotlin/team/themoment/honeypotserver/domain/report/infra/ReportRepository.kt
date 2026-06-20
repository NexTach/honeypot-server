package team.themoment.honeypotserver.domain.report.infra

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import team.themoment.honeypotserver.domain.report.domain.Report
import team.themoment.honeypotserver.domain.report.domain.ReportStatus

interface ReportRepository : JpaRepository<Report, Long> {
    fun existsByReporterIdAndGifId(
        reporterId: Long,
        gifId: Long,
    ): Boolean

    fun existsByReporterIdAndGifIdAndStatus(
        reporterId: Long,
        gifId: Long,
        status: ReportStatus,
    ): Boolean

    @EntityGraph(attributePaths = ["reporter", "gif", "processedBy"])
    @Query("select r from Report r where r.status = :status")
    fun findByStatusWithAssociations(
        status: ReportStatus,
        pageable: Pageable,
    ): Page<Report>

    @EntityGraph(attributePaths = ["reporter", "gif", "processedBy"])
    override fun findAll(pageable: Pageable): Page<Report>
}
