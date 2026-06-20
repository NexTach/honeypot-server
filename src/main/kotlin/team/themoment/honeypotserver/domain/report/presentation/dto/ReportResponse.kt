package team.themoment.honeypotserver.domain.report.presentation.dto

import team.themoment.honeypotserver.domain.report.domain.Report
import team.themoment.honeypotserver.domain.report.domain.ReportStatus
import java.time.Instant

data class ReportResponse(
    val id: Long,
    val reporterId: Long,
    val gifId: Long,
    val reasonTitle: String,
    val detail: String,
    val status: ReportStatus,
    val processedById: Long?,
    val processedAt: Instant?,
    val createdAt: Instant,
) {
    companion object {
        fun from(report: Report): ReportResponse =
            ReportResponse(
                id = report.id,
                reporterId = report.reporter.id,
                gifId = report.gif.id,
                reasonTitle = report.reasonTitle,
                detail = report.detail,
                status = report.status,
                processedById = report.processedBy?.id,
                processedAt = report.processedAt,
                createdAt = report.createdAt,
            )
    }
}
