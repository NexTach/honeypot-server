package team.themoment.honeypotserver.domain.report.presentation

import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import team.themoment.honeypotserver.domain.report.application.ReportService
import team.themoment.honeypotserver.domain.report.domain.ReportStatus
import team.themoment.honeypotserver.domain.report.presentation.dto.ProcessReportRequest
import team.themoment.honeypotserver.domain.report.presentation.dto.ReportResponse
import team.themoment.honeypotserver.global.security.AuthPrincipal
import team.themoment.honeypotserver.global.security.CurrentUser

@RestController
@RequestMapping("/v1/admin/reports")
class AdminReportController(
    private val reportService: ReportService,
) {

    @GetMapping
    fun getReports(
        @RequestParam(required = false) status: ReportStatus?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<ReportResponse> {
        return reportService.getReports(status, pageable).map { ReportResponse.from(it) }
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun processReport(
        @PathVariable id: Long,
        @Valid @RequestBody request: ProcessReportRequest,
        @CurrentUser principal: AuthPrincipal,
    ) {
        reportService.processReport(
            reportId = id,
            action = request.action,
            adminPrincipal = principal,
        )
    }
}
