package team.themoment.honeypotserver.domain.like.presentation.dto.response

import org.springframework.data.domain.Page
import team.themoment.honeypotserver.domain.gif.presentation.dto.response.GifResponse

data class LikePageResponse(
    val content: List<GifResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
) {
    companion object {
        fun from(page: Page<GifResponse>): LikePageResponse =
            LikePageResponse(
                content = page.content,
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                last = page.isLast,
            )
    }
}
