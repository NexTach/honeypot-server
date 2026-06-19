package team.themoment.honeypotserver.domain.report.presentation.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateReportRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val reasonTitle: String,

    @field:NotBlank
    @field:Size(max = 1000)
    val detail: String,
)
