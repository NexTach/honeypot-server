package team.themoment.honeypotserver.domain.gif.presentation

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import team.themoment.honeypotserver.domain.gif.application.GifQueryService
import team.themoment.honeypotserver.domain.gif.presentation.dto.response.GifResponse
import team.themoment.honeypotserver.global.security.AuthPrincipal
import team.themoment.honeypotserver.global.security.CurrentUser

/**
 * 마이페이지 — 내가 올린 GIF 목록.
 * UserController 를 건드리지 않고 별도 컨트롤러로 분리.
 * - 비공개(isPublic = false) 및 관리자 블라인드(blindedByAdmin = true) GIF 도 포함하여 상태를 표기.
 * - GifResponse 에 isPublic / blindedByAdmin 이 이미 포함되어 있으므로 그대로 활용.
 */
@RestController
@RequestMapping("/v1/users/me/gifs")
class MyGifController(
    private val gifQueryService: GifQueryService,
) {
    @GetMapping
    fun getMyGifs(
        @PageableDefault(size = 20) pageable: Pageable,
        @CurrentUser principal: AuthPrincipal,
    ): Page<GifResponse> =
        gifQueryService
            .getMyGifs(
                principal = principal,
                pageable = pageable,
            ).map { GifResponse.from(it) }
}
