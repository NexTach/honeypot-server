package team.themoment.honeypotserver.domain.report.presentation.dto

import jakarta.validation.constraints.NotNull
import team.themoment.honeypotserver.domain.report.domain.ReportAction

data class ProcessReportRequest(
    @field:NotNull
    val action: ReportAction,
)
