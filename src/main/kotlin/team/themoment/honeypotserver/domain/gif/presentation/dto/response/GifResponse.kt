package team.themoment.honeypotserver.domain.gif.presentation.dto.response

import team.themoment.honeypotserver.domain.gif.domain.Gif
import team.themoment.honeypotserver.domain.tag.presentation.dto.response.TagResponse
import java.time.Instant

data class GifResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val isPublic: Boolean,
    val blindedByAdmin: Boolean,
    val objectKey: String,
    val contentType: String,
    val fileSize: Long,
    val width: Int?,
    val height: Int?,
    val likeCount: Long,
    val shareCount: Long,
    val uploaderId: Long,
    val uploaderName: String,
    val tags: List<TagResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(gif: Gif): GifResponse =
            GifResponse(
                id = gif.id,
                title = gif.title,
                description = gif.description,
                isPublic = gif.isPublic,
                blindedByAdmin = gif.blindedByAdmin,
                objectKey = gif.objectKey,
                contentType = gif.contentType,
                fileSize = gif.fileSize,
                width = gif.width,
                height = gif.height,
                likeCount = gif.likeCount,
                shareCount = gif.shareCount,
                uploaderId = gif.uploader.id,
                uploaderName = gif.uploader.name,
                tags = gif.tags.map { TagResponse.from(it) },
                createdAt = gif.createdAt,
                updatedAt = gif.updatedAt,
            )
    }
}
