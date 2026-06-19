package team.themoment.honeypotserver.domain.gif.application

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.themoment.honeypotserver.domain.gif.domain.Gif
import team.themoment.honeypotserver.domain.gif.domain.GifRepository
import team.themoment.honeypotserver.domain.user.domain.Role
import team.themoment.honeypotserver.global.security.AuthPrincipal
import team.themoment.sdk.exception.ExpectedException

@Service
@Transactional(readOnly = true)
class GifQueryService(
    private val gifRepository: GifRepository,
) {

    fun getGifDetail(gifId: Long, principal: AuthPrincipal?): Gif {
        val gif = gifRepository.findById(gifId).orElseThrow {
            ExpectedException("GIF not found", HttpStatus.NOT_FOUND)
        }
        checkVisibility(gif, principal)
        return gif
    }

    fun checkVisibility(gif: Gif, principal: AuthPrincipal?) {
        if (principal != null && principal.role == Role.ADMIN) return

        if (gif.blindedByAdmin) {
            throw ExpectedException("GIF not found", HttpStatus.NOT_FOUND)
        }

        if (!gif.isPublic) {
            if (principal == null || gif.uploader.id != principal.userId) {
                throw ExpectedException("GIF not found", HttpStatus.NOT_FOUND)
            }
        }
    }

    // NOTE: List/search methods (findAll, findByUploader, search by keyword/tag) are
    // intentionally left for the Search worker to add here as additional methods.
}
