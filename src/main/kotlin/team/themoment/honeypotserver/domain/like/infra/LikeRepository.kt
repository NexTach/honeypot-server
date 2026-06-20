package team.themoment.honeypotserver.domain.like.infra

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import team.themoment.honeypotserver.domain.like.domain.Like

interface LikeRepository : JpaRepository<Like, Long> {
    fun existsByUserIdAndGifId(
        userId: Long,
        gifId: Long,
    ): Boolean

    @Query(
        value = "select l from Like l join fetch l.gif g join fetch g.uploader join fetch g.tags where l.user.id = :userId",
        countQuery = "select count(l) from Like l where l.user.id = :userId",
    )
    fun findByUserId(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): Page<Like>

    @Modifying
    @Transactional
    @Query("delete from Like l where l.user.id = :userId and l.gif.id = :gifId")
    fun deleteByUserIdAndGifId(
        @Param("userId") userId: Long,
        @Param("gifId") gifId: Long,
    ): Int
}
