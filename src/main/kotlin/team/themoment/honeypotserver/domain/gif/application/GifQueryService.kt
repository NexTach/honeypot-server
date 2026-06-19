package team.themoment.honeypotserver.domain.gif.application

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.themoment.honeypotserver.domain.gif.domain.Gif
import team.themoment.honeypotserver.domain.gif.domain.GifRepository
import team.themoment.honeypotserver.domain.gif.infra.GifQueryRepository
import team.themoment.honeypotserver.domain.user.domain.Role
import team.themoment.honeypotserver.global.security.AuthPrincipal
import team.themoment.sdk.exception.ExpectedException

private const val DEFAULT_PAGE_SIZE = 20
private const val MAX_PAGE_SIZE = 100

@Service
@Transactional(readOnly = true)
class GifQueryService(
    private val gifRepository: GifRepository,
    private val gifQueryRepository: GifQueryRepository,
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

    /**
     * GIF 검색/목록.
     * - 가시성 필터: blindedByAdmin = false AND (isPublic = true OR uploader = me)
     * - keyword: title LIKE %keyword% OR tag.name = normalized(keyword)
     * - sort: "latest"(기본) / "popular"
     * - size 최대 100 강제
     */
    fun searchGifs(
        keyword: String?,
        sort: String,
        principal: AuthPrincipal,
        pageable: Pageable,
    ): Page<Gif> {
        val safePageable = capPageSize(pageable)
        return gifQueryRepository.search(
            keyword = keyword,
            sort = sort,
            viewerId = principal.userId,
            pageable = safePageable,
        )
    }

    /**
     * 내가 올린 GIF 목록 (비공개·blinded 포함, 마이페이지용).
     */
    fun getMyGifs(principal: AuthPrincipal, pageable: Pageable): Page<Gif> {
        val safePageable = capPageSize(pageable)
        return gifQueryRepository.findByUploader(
            uploaderId = principal.userId,
            pageable = safePageable,
        )
    }

    private fun capPageSize(pageable: Pageable): Pageable {
        val size = pageable.pageSize.coerceIn(1, MAX_PAGE_SIZE)
        return if (size == pageable.pageSize) pageable
        else PageRequest.of(pageable.pageNumber, size, pageable.sort)
    }
}
