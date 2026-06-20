package team.themoment.honeypotserver.domain.report.application

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.themoment.honeypotserver.domain.gif.application.GifCommandService
import team.themoment.honeypotserver.domain.gif.domain.GifRepository
import team.themoment.honeypotserver.domain.report.domain.Report
import team.themoment.honeypotserver.domain.report.domain.ReportAction
import team.themoment.honeypotserver.domain.report.domain.ReportStatus
import team.themoment.honeypotserver.domain.report.infra.ReportRepository
import team.themoment.honeypotserver.domain.user.application.UserService
import team.themoment.honeypotserver.global.security.AuthPrincipal
import team.themoment.sdk.exception.ExpectedException
import java.time.Instant

@Service
@Transactional
class ReportService(
    private val reportRepository: ReportRepository,
    private val gifCommandService: GifCommandService,
    // Direct GifRepository access is needed because GifCommandService does not expose
    // a blindGif() operation (foundation file, cannot be modified). When a blindGif
    // method is added to GifCommandService, remove this dependency.
    private val gifRepository: GifRepository,
    private val userService: UserService,
) {
    fun createReport(
        gifId: Long,
        reasonTitle: String,
        detail: String,
        principal: AuthPrincipal,
    ): Report {
        val gif = gifCommandService.findGifOrThrow(gifId)

        if (gif.uploader.id == principal.userId) {
            throw ExpectedException("자신의 GIF는 신고할 수 없습니다.", HttpStatus.BAD_REQUEST)
        }

        // Block reporting a GIF that an admin has already hidden.
        if (gif.blindedByAdmin) {
            throw ExpectedException("이미 관리자에 의해 블라인드 처리된 GIF입니다.", HttpStatus.BAD_REQUEST)
        }

        // Block any duplicate report from the same reporter against the same GIF
        // regardless of the prior report's resolution status, to prevent repeated
        // re-reports after each NO_ISSUE resolution.
        if (reportRepository.existsByReporterIdAndGifId(principal.userId, gifId)) {
            throw ExpectedException("이미 해당 GIF에 대한 신고가 존재합니다.", HttpStatus.CONFLICT)
        }

        val reporter = userService.getById(principal.userId)

        val report =
            Report(
                reporter = reporter,
                gif = gif,
                reasonTitle = reasonTitle,
                detail = detail,
                status = ReportStatus.PENDING,
            )

        return reportRepository.save(report)
    }

    @Transactional(readOnly = true)
    fun getReports(
        status: ReportStatus?,
        pageable: Pageable,
    ): Page<Report> =
        if (status != null) {
            reportRepository.findByStatusWithAssociations(status, pageable)
        } else {
            reportRepository.findAll(pageable)
        }

    fun processReport(
        reportId: Long,
        action: ReportAction,
        adminPrincipal: AuthPrincipal,
    ) {
        val report =
            reportRepository.findById(reportId).orElseThrow {
                ExpectedException("신고를 찾을 수 없습니다.", HttpStatus.NOT_FOUND)
            }

        // Guard against re-processing an already-resolved report to prevent status
        // regression (e.g., BLINDED → NO_ISSUE while the GIF remains blinded).
        if (report.status != ReportStatus.PENDING) {
            throw ExpectedException("이미 처리된 신고입니다.", HttpStatus.CONFLICT)
        }

        val now = Instant.now()

        when (action) {
            ReportAction.NO_ISSUE -> {
                val admin = userService.getById(adminPrincipal.userId)
                report.status = ReportStatus.NO_ISSUE
                report.processedBy = admin
                report.processedAt = now
                // report is a managed entity — JPA dirty-checking flushes on commit.
            }
            ReportAction.BLIND -> {
                val admin = userService.getById(adminPrincipal.userId)
                gifRepository.updateBlindedByAdmin(report.gif.id, true)
                report.status = ReportStatus.BLINDED
                report.processedBy = admin
                report.processedAt = now
                // report is a managed entity — JPA dirty-checking flushes on commit.
            }
            ReportAction.DELETE -> {
                // forceDeleteGifById cascade-deletes all Report rows for this GIF
                // at the DB level via @OnDelete(CASCADE) on the gif FK.
                // The current report entity will also be removed — no explicit
                // status update is needed or possible after this call.
                gifCommandService.forceDeleteGifById(report.gif.id)
            }
        }
    }
}
