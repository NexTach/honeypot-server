package team.themoment.honeypotserver.domain.gif.domain

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GifRepository {
    fun findById(id: Long): java.util.Optional<Gif>

    fun save(gif: Gif): Gif

    fun delete(gif: Gif)

    @Modifying
    @Query("update Gif g set g.likeCount = g.likeCount + 1 where g.id = :id")
    fun incrementLikeCount(
        @Param("id") id: Long,
    )

    @Modifying
    @Query("update Gif g set g.likeCount = g.likeCount - 1 where g.id = :id")
    fun decrementLikeCount(
        @Param("id") id: Long,
    )

    @Modifying
    @Query("update Gif g set g.shareCount = g.shareCount + 1 where g.id = :id")
    fun incrementShareCount(
        @Param("id") id: Long,
    )

    @Modifying
    @Query("update Gif g set g.blindedByAdmin = :blinded where g.id = :id")
    fun updateBlindedByAdmin(
        @Param("id") id: Long,
        @Param("blinded") blinded: Boolean,
    )
}
