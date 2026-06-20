package team.themoment.honeypotserver.domain.gif.infra

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import team.themoment.honeypotserver.domain.gif.domain.Gif
import team.themoment.honeypotserver.domain.gif.domain.GifQueryRepository

@Repository
class GifQueryRepositoryImpl(
    @PersistenceContext
    private val em: EntityManager,
) : GifQueryRepository {
    /**
     * 가시성 필터 + keyword 검색 + 정렬 + 페이지네이션.
     * 태그 조인 시 중복 행이 생기므로 DISTINCT 처리. 카운트 쿼리는 별도(서브쿼리) 방식으로.
     */
    override fun search(
        keyword: String?,
        sort: String,
        viewerId: Long,
        pageable: Pageable,
    ): Page<Gif> {
        val normalizedKeyword = keyword?.trim()?.lowercase()

        // ─── 결과 쿼리 ───────────────────────────────────────────────────────────
        val jpql =
            buildString {
                append("SELECT DISTINCT g FROM Gif g")
                if (!normalizedKeyword.isNullOrBlank()) {
                    append(" LEFT JOIN g.tags t")
                }
                append(" WHERE g.blindedByAdmin = false")
                append(" AND (g.isPublic = true OR g.uploader.id = :viewerId)")
                if (!normalizedKeyword.isNullOrBlank()) {
                    append(" AND (LOWER(g.title) LIKE :titleLike OR LOWER(t.name) = :tagName)")
                }
                val orderBy =
                    when (sort) {
                        "popular" -> "g.likeCount DESC, g.id DESC"
                        else -> "g.createdAt DESC, g.id DESC"
                    }
                append(" ORDER BY $orderBy")
            }

        val query = em.createQuery(jpql, Gif::class.java)
        query.setParameter("viewerId", viewerId)
        if (!normalizedKeyword.isNullOrBlank()) {
            query.setParameter("titleLike", "%$normalizedKeyword%")
            query.setParameter("tagName", normalizedKeyword)
        }

        val offset = pageable.offset.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val pageSize = pageable.pageSize
        query.firstResult = offset
        query.maxResults = pageSize

        val resultList = query.resultList

        // ─── 카운트 쿼리 ─────────────────────────────────────────────────────────
        val countJpql =
            buildString {
                append("SELECT COUNT(DISTINCT g.id) FROM Gif g")
                if (!normalizedKeyword.isNullOrBlank()) {
                    append(" LEFT JOIN g.tags t")
                }
                append(" WHERE g.blindedByAdmin = false")
                append(" AND (g.isPublic = true OR g.uploader.id = :viewerId)")
                if (!normalizedKeyword.isNullOrBlank()) {
                    append(" AND (LOWER(g.title) LIKE :titleLike OR LOWER(t.name) = :tagName)")
                }
            }

        val countQuery = em.createQuery(countJpql, Long::class.java)
        countQuery.setParameter("viewerId", viewerId)
        if (!normalizedKeyword.isNullOrBlank()) {
            countQuery.setParameter("titleLike", "%$normalizedKeyword%")
            countQuery.setParameter("tagName", normalizedKeyword)
        }
        val total = countQuery.singleResult

        return PageImpl(resultList, pageable, total)
    }

    /**
     * 업로더의 전체 GIF (마이페이지용, 비공개/blinded 포함).
     */
    override fun findByUploader(
        uploaderId: Long,
        pageable: Pageable,
    ): Page<Gif> {
        val jpql = "SELECT g FROM Gif g WHERE g.uploader.id = :uploaderId ORDER BY g.createdAt DESC, g.id DESC"

        val query = em.createQuery(jpql, Gif::class.java)
        query.setParameter("uploaderId", uploaderId)
        query.firstResult = pageable.offset.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        query.maxResults = pageable.pageSize

        val resultList = query.resultList

        val countJpql = "SELECT COUNT(g) FROM Gif g WHERE g.uploader.id = :uploaderId"
        val countQuery = em.createQuery(countJpql, Long::class.java)
        countQuery.setParameter("uploaderId", uploaderId)
        val total = countQuery.singleResult

        return PageImpl(resultList, pageable, total)
    }
}
