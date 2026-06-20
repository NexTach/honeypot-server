package team.themoment.honeypotserver.domain.report.presentation

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import team.themoment.honeypotserver.domain.report.application.ReportService
import team.themoment.honeypotserver.domain.report.presentation.dto.CreateReportRequest
import team.themoment.honeypotserver.domain.report.presentation.dto.ReportResponse
import team.themoment.honeypotserver.global.security.AuthPrincipal
import team.themoment.honeypotserver.global.security.CurrentUser

@RestController
@RequestMapping("/v1/gifs")
class ReportController(
    private val reportService: ReportService,
) {
    @PostMapping("/{gifId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    fun createReport(
        @PathVariable gifId: Long,
        @Valid @RequestBody request: CreateReportRequest,
        @CurrentUser principal: AuthPrincipal,
    ): ReportResponse {
        val report =
            reportService.createReport(
                gifId = gifId,
                reasonTitle = request.reasonTitle,
                detail = request.detail,
                principal = principal,
            )
        return ReportResponse.from(report)
    }
}
