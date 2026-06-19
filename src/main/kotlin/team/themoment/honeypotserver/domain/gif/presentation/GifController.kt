package team.themoment.honeypotserver.domain.gif.presentation

import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
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
) {

    @GetMapping
    fun listGifs(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "latest") sort: String,
        @PageableDefault(size = 20) pageable: Pageable,
        @CurrentUser principal: AuthPrincipal,
    ): Page<GifResponse> {
        return gifQueryService.searchGifs(
            keyword = keyword,
            sort = sort,
            principal = principal,
            pageable = pageable,
        ).map { GifResponse.from(it) }
    }

    @PostMapping(consumes = ["multipart/form-data"])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadGif(
        @RequestPart("file") file: MultipartFile,
        @RequestPart("title") title: String,
        @RequestPart(value = "description", required = false) description: String?,
        @RequestPart(value = "isPublic", required = false) isPublic: String?,
        @RequestPart(value = "tags", required = false) tags: List<String>?,
        @CurrentUser principal: AuthPrincipal,
    ): GifResponse {
        val gif = gifCommandService.upload(
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

    @GetMapping("/{id}/raw")
    fun getGifRaw(
        @PathVariable id: Long,
        @CurrentUser principal: AuthPrincipal,
        response: HttpServletResponse,
    ) {
        val presignedUrl = gifMediaService.getPresignedUrl(id, principal)
        response.sendRedirect(presignedUrl.toString())
    }

    @PatchMapping("/{id}")
    fun updateGif(
        @PathVariable id: Long,
        @Valid @org.springframework.web.bind.annotation.RequestBody request: GifUpdateRequest,
        @CurrentUser principal: AuthPrincipal,
    ): GifResponse {
        val gif = gifCommandService.updateMetadata(
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
