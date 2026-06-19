package team.themoment.honeypotserver.domain.tag.application

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import team.themoment.honeypotserver.domain.tag.domain.Tag
import team.themoment.honeypotserver.domain.tag.infra.TagRepository
import team.themoment.sdk.exception.ExpectedException

@Service
class TagService(
    private val tagRepository: TagRepository,
) {

    @Transactional
    fun findOrCreateTags(rawNames: List<String>): List<Tag> {
        if (rawNames.size > MAX_TAGS_PER_GIF) {
            throw ExpectedException(
                "Too many tags: maximum $MAX_TAGS_PER_GIF tags allowed per GIF",
                HttpStatus.BAD_REQUEST,
            )
        }

        val normalized = rawNames.map { normalize(it) }.distinct()

        return normalized.map { name ->
            tagRepository.findByName(name) ?: tagRepository.save(Tag(name = name))
        }
    }

    @Transactional(readOnly = true)
    fun searchByKeyword(keyword: String): List<Tag> =
        tagRepository.findByNameContainingIgnoreCase(keyword)

    private fun normalize(name: String): String {
        val trimmed = name.trim().lowercase()
        if (trimmed.length > MAX_TAG_LENGTH) {
            throw ExpectedException(
                "Tag name too long: maximum $MAX_TAG_LENGTH characters",
                HttpStatus.BAD_REQUEST,
            )
        }
        if (trimmed.isEmpty()) {
            throw ExpectedException("Tag name must not be empty", HttpStatus.BAD_REQUEST)
        }
        return trimmed
    }

    companion object {
        const val MAX_TAGS_PER_GIF = 20
        const val MAX_TAG_LENGTH = 30
    }
}
