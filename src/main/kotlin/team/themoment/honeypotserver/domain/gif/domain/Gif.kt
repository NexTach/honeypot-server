package team.themoment.honeypotserver.domain.gif.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.Lob
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import team.themoment.honeypotserver.domain.tag.domain.Tag
import team.themoment.honeypotserver.domain.user.domain.User
import team.themoment.honeypotserver.global.common.BaseTimeEntity

@Entity
@Table(
    name = "gifs",
    indexes = [
        Index(name = "idx_gif_created_at", columnList = "created_at"),
        Index(name = "idx_gif_like_count", columnList = "like_count"),
        Index(name = "idx_gif_uploader_id", columnList = "uploader_id"),
    ],
)
class Gif(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false, length = 100)
    var title: String,
    @Lob
    @Column(nullable = true, columnDefinition = "TEXT")
    var description: String? = null,
    @Column(nullable = false)
    var isPublic: Boolean = true,
    @Column(nullable = false)
    var blindedByAdmin: Boolean = false,
    @Column(nullable = false)
    val objectKey: String,
    @Column(nullable = false, length = 100)
    val contentType: String,
    @Column(nullable = false)
    val fileSize: Long,
    @Column(nullable = true)
    val width: Int? = null,
    @Column(nullable = true)
    val height: Int? = null,
    @Column(nullable = false, name = "like_count")
    var likeCount: Long = 0,
    @Column(nullable = false, name = "share_count")
    var shareCount: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    val uploader: User,
    @ManyToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    @JoinTable(
        name = "gif_tags",
        joinColumns = [JoinColumn(name = "gif_id")],
        inverseJoinColumns = [JoinColumn(name = "tag_id")],
    )
    var tags: MutableList<Tag> = mutableListOf(),
) : BaseTimeEntity() {
    fun updateMetadata(
        title: String,
        description: String?,
        isPublic: Boolean,
        tags: MutableList<Tag>,
    ) {
        this.title = title
        this.description = description
        this.isPublic = isPublic
        this.tags = tags
    }
}
