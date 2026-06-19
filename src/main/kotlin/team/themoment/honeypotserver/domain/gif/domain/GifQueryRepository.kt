package team.themoment.honeypotserver.domain.gif.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface GifQueryRepository {

    /**
     * 가시성 필터가 적용된 GIF 검색/목록.
     * - blindedByAdmin = false AND (isPublic = true OR uploader.id = viewerId)
     * - keyword 있으면: title LIKE %keyword% OR tag.name = normalized(keyword)
     * - sort: "latest"(createdAt DESC, id DESC) / "popular"(likeCount DESC, id DESC)
     */
    fun search(
        keyword: String?,
        sort: String,
        viewerId: Long,
        pageable: Pageable,
    ): Page<Gif>

    /**
     * 특정 업로더의 모든 GIF (비공개·blinded 포함, 마이페이지용).
     */
    fun findByUploader(uploaderId: Long, pageable: Pageable): Page<Gif>
}
