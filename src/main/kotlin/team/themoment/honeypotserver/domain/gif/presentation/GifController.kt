package team.themoment.honeypotserver.domain.gif.presentation

import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import team.themoment.honeypotserver.domain.gif.application.GifCommandService
import team.themoment.honeypotserver.domain.gif.application.GifMediaService
import team.themoment.honeypotserver.domain.gif.application.GifQueryService
import team.themoment.honeypotserver.domain.gif.presentation.dto.request.GifUpdateRequest
import team.themoment.honeypotserver.domain.gif.presentation.dto.response.GifResponse
import team.themoment.honeypotserver.domain.gif.presentation.dto.response.ShareResponse
import team.themoment.honeypotserver.global.security.AuthPrincipal
import team.themoment.honeypotserver.global.security.CurrentUser

@RestController
@RequestMapping("/v1/gifs")
class GifController(
    private val gifCommandService: GifCommandService,
    private val gifQueryService: GifQueryService,
    private val gifMediaService: GifMediaService,
    @Value("\${honeypot.media.public-cache-ttl:60}") private val publicCacheTtlSeconds: Long,
) {
    @GetMapping
    fun listGifs(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "latest") sort: String,
        @PageableDefault(size = 20) pageable: Pageable,
        @CurrentUser principal: AuthPrincipal,
    ): Page<GifResponse> =
        gifQueryService
            .searchGifs(
                keyword = keyword,
                sort = sort,
                principal = principal,
                pageable = pageable,
            ).map { GifResponse.from(it) }

    @PostMapping(consumes = ["multipart/form-data"])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadGif(
        @RequestPart("file") file: MultipartFile,
        @RequestParam("title") title: String,
        @RequestParam(value = "description", required = false) description: String?,
        @RequestParam(value = "isPublic", required = false) isPublic: String?,
        @RequestParam(value = "tags", required = false) tags: List<String>?,
        @CurrentUser principal: AuthPrincipal,
    ): GifResponse {
        val gif =
            gifCommandService.upload(
                file = file,
                title = title,
                description = description,
                isPublic = isPublic?.toBoolean() ?: true,
                tagNames = tags ?: emptyList(),
                principal = principal,
            )
        return GifResponse.from(gif)
    }

    @GetMapping("/{id}")
    fun getGif(
        @PathVariable id: Long,
        @CurrentUser principal: AuthPrincipal,
    ): GifResponse {
        val gif = gifQueryService.getGifDetail(id, principal)
        return GifResponse.from(gif)
    }

    /**
     * GIF 원본을 백엔드가 직접 중계(프록시)한다. 공개 GIF 는 인증 없이(`permitAll`) 접근 가능해
     * 외부 메신저(Discord 등)가 링크로 임베드할 수 있다. 비공개/블라인드는 [GifMediaService] 의
     * 가시성 인가에서 404 로 떨어진다(미인증 익명 포함).
     */
    @GetMapping("/{id}/raw")
    fun getGifRaw(
        @PathVariable id: Long,
        @CurrentUser principal: AuthPrincipal?,
        response: HttpServletResponse,
    ) {
        val stream = gifMediaService.openAuthorizedStream(id, principal)
        stream.body.use { body ->
            response.contentType = stream.contentType
            response.setContentLengthLong(stream.contentLength)
            response.setHeader(
                HttpHeaders.CACHE_CONTROL,
                if (stream.isPublic) "public, max-age=$publicCacheTtlSeconds" else "private, no-store",
            )
            body.copyTo(response.outputStream)
        }
    }

    @PatchMapping("/{id}")
    fun updateGif(
        @PathVariable id: Long,
        @Valid @org.springframework.web.bind.annotation.RequestBody request: GifUpdateRequest,
        @CurrentUser principal: AuthPrincipal,
    ): GifResponse {
        val gif =
            gifCommandService.updateMetadata(
                gifId = id,
                title = request.title,
                description = request.description,
                isPublic = request.isPublic,
                tagNames = request.tags,
                principal = principal,
            )
        return GifResponse.from(gif)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteGif(
        @PathVariable id: Long,
        @CurrentUser principal: AuthPrincipal,
    ) {
        gifCommandService.deleteGif(id, principal)
    }

    @PostMapping("/{id}/share")
    fun shareGif(
        @PathVariable id: Long,
    ): ShareResponse {
        val shareCount = gifCommandService.incrementShare(id)
        return ShareResponse(shareCount = shareCount)
    }
}
