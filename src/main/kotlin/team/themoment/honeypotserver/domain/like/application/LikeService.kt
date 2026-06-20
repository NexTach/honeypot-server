package team.themoment.honeypotserver.domain.like.application

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.themoment.honeypotserver.domain.gif.application.GifQueryService
import team.themoment.honeypotserver.domain.gif.domain.GifRepository
import team.themoment.honeypotserver.domain.gif.presentation.dto.response.GifResponse
import team.themoment.honeypotserver.domain.like.domain.Like
import team.themoment.honeypotserver.domain.like.infra.LikeRepository
import team.themoment.honeypotserver.domain.user.application.UserService
import team.themoment.honeypotserver.global.security.AuthPrincipal

@Service
@Transactional
class LikeService(
    private val likeRepository: LikeRepository,
    private val gifRepository: GifRepository,
    private val gifQueryService: GifQueryService,
    private val userService: UserService,
) {
    fun like(
        gifId: Long,
        principal: AuthPrincipal,
    ) {
        val gif = gifQueryService.getGifDetail(gifId, principal)

        if (likeRepository.existsByUserIdAndGifId(principal.userId, gifId)) {
            return
        }

        val user = userService.getById(principal.userId)
        likeRepository.save(Like(user = user, gif = gif))
        // We do NOT catch DataIntegrityViolationException here: catching it after the
        // Hibernate Session has seen a constraint violation leaves the Session broken,
        // preventing the subsequent incrementLikeCount call. The pre-check above handles
        // the common idempotent case; a concurrent duplicate is rare and the resulting
        // 409/500 response is acceptable.
        gifRepository.incrementLikeCount(gifId)
    }

    fun unlike(
        gifId: Long,
        principal: AuthPrincipal,
    ) {
        // Use a JPQL DELETE that returns affected row count to avoid the TOCTOU race
        // between an existence check and the actual delete. If 0 rows were deleted
        // (no like existed or a concurrent unlike beat us), we skip the counter update.
        val deleted = likeRepository.deleteByUserIdAndGifId(principal.userId, gifId)
        if (deleted > 0) {
            gifRepository.decrementLikeCount(gifId)
        }
    }

    @Transactional(readOnly = true)
    fun getMyLikes(
        principal: AuthPrincipal,
        pageable: Pageable,
    ): Page<GifResponse> =
        likeRepository
            .findByUserId(principal.userId, pageable)
            .map { like -> GifResponse.from(like.gif) }
}
