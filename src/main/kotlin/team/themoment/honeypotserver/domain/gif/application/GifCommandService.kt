package team.themoment.honeypotserver.domain.gif.application

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import team.themoment.honeypotserver.domain.gif.domain.Gif
import team.themoment.honeypotserver.domain.gif.domain.GifRepository
import team.themoment.honeypotserver.domain.gif.infra.GifStorageAdapter
import team.themoment.honeypotserver.domain.tag.application.TagService
import team.themoment.honeypotserver.domain.user.application.UserService
import team.themoment.honeypotserver.global.security.AuthPrincipal
import team.themoment.sdk.exception.ExpectedException

@Service
class GifCommandService(
    private val gifRepository: GifRepository,
    private val gifStorageAdapter: GifStorageAdapter,
    private val tagService: TagService,
    private val userService: UserService,
    @Value("\${honeypot.upload.max-size}") private val maxUploadSize: Long,
) {

    @Transactional
    fun upload(
        file: MultipartFile,
        title: String,
        description: String?,
        isPublic: Boolean,
        tagNames: List<String>,
        principal: AuthPrincipal,
    ): Gif {
        val bytes = file.bytes
        validateGifFile(file.contentType, bytes, bytes.size.toLong())

        val uploader = userService.getById(principal.userId)
        val tags = tagService.findOrCreateTags(tagNames).toMutableList()

        // Generate key first (no I/O yet), save metadata to DB, then upload to S3.
        // This way a DB failure prevents an orphaned S3 object.
        val objectKey = gifStorageAdapter.generateKey()

        val gif = Gif(
            title = title,
            description = description,
            isPublic = isPublic,
            objectKey = objectKey,
            contentType = GIF_CONTENT_TYPE,
            fileSize = bytes.size.toLong(),
            uploader = uploader,
            tags = tags,
        )

        val saved = gifRepository.save(gif)

        // Upload after DB row is persisted — S3 failure here leaves a dangling row
        // (no file), but that is recoverable unlike the reverse (orphaned file).
        gifStorageAdapter.upload(objectKey, bytes, GIF_CONTENT_TYPE)

        return saved
    }

    @Transactional
    fun updateMetadata(
        gifId: Long,
        title: String,
        description: String?,
        isPublic: Boolean,
        tagNames: List<String>,
        principal: AuthPrincipal,
    ): Gif {
        val gif = findGifOrThrow(gifId)
        checkOwnership(gif, principal)

        val tags = tagService.findOrCreateTags(tagNames).toMutableList()
        gif.updateMetadata(title, description, isPublic, tags)

        return gifRepository.save(gif)
    }

    @Transactional
    fun deleteGif(gifId: Long, principal: AuthPrincipal) {
        val gif = findGifOrThrow(gifId)
        checkOwnership(gif, principal)
        forceDeleteGifById(gifId)
    }

    /**
     * Force-deletes a GIF by ID without ownership check.
     * Used by admin operations (e.g., Report worker calling admin DELETE).
     * Accepts an ID rather than an entity to avoid detached-entity issues
     * when the caller loaded the entity in a different transaction.
     */
    @Transactional
    fun forceDeleteGifById(gifId: Long) {
        val gif = findGifOrThrow(gifId)
        gifStorageAdapter.delete(gif.objectKey)
        gifRepository.delete(gif)
    }

    @Transactional
    fun incrementShare(gifId: Long): Long {
        findGifOrThrow(gifId) // validate existence
        gifRepository.incrementShareCount(gifId)
        // Re-fetch after the bulk UPDATE to return the correct post-update value.
        return findGifOrThrow(gifId).shareCount
    }

    fun findGifOrThrow(gifId: Long): Gif =
        gifRepository.findById(gifId).orElseThrow {
            ExpectedException("GIF not found", HttpStatus.NOT_FOUND)
        }

    private fun checkOwnership(gif: Gif, principal: AuthPrincipal) {
        if (gif.uploader.id != principal.userId) {
            throw ExpectedException("You do not have permission to modify this GIF", HttpStatus.FORBIDDEN)
        }
    }

    private fun validateGifFile(contentType: String?, bytes: ByteArray, fileSize: Long) {
        if (contentType != GIF_CONTENT_TYPE) {
            throw ExpectedException("Only GIF files are allowed", HttpStatus.BAD_REQUEST)
        }
        if (fileSize > maxUploadSize) {
            throw ExpectedException("File size exceeds maximum allowed size", HttpStatus.BAD_REQUEST)
        }
        if (!hasGifMagicBytes(bytes)) {
            throw ExpectedException("File does not appear to be a valid GIF", HttpStatus.BAD_REQUEST)
        }
    }

    private fun hasGifMagicBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 6) return false
        // GIF header: G I F 3 {7|9} a
        val commonPrefix = bytes[0] == 0x47.toByte() &&
            bytes[1] == 0x49.toByte() &&
            bytes[2] == 0x46.toByte() &&
            bytes[3] == 0x38.toByte() &&
            bytes[5] == 0x61.toByte()
        val isGif87a = bytes[4] == 0x37.toByte() // GIF87a
        val isGif89a = bytes[4] == 0x39.toByte() // GIF89a
        return commonPrefix && (isGif87a || isGif89a)
    }

    companion object {
        private const val GIF_CONTENT_TYPE = "image/gif"
    }
}
