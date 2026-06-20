package team.themoment.honeypotserver.domain.like.presentation

import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import team.themoment.honeypotserver.domain.like.application.LikeService
import team.themoment.honeypotserver.domain.like.presentation.dto.response.LikePageResponse
import team.themoment.honeypotserver.global.security.AuthPrincipal
import team.themoment.honeypotserver.global.security.CurrentUser

@RestController
class LikeController(
    private val likeService: LikeService,
) {
    @PostMapping("/v1/gifs/{gifId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun like(
        @PathVariable gifId: Long,
        @CurrentUser principal: AuthPrincipal,
    ) {
        likeService.like(gifId, principal)
    }

    @DeleteMapping("/v1/gifs/{gifId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unlike(
        @PathVariable gifId: Long,
        @CurrentUser principal: AuthPrincipal,
    ) {
        likeService.unlike(gifId, principal)
    }

    @GetMapping("/v1/users/me/likes")
    fun getMyLikes(
        @CurrentUser principal: AuthPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): LikePageResponse {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 100)
        val pageable = PageRequest.of(safePage, safeSize)
        val result = likeService.getMyLikes(principal, pageable)
        return LikePageResponse.from(result)
    }
}
