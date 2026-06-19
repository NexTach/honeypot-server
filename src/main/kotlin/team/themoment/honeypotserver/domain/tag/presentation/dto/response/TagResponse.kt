package team.themoment.honeypotserver.domain.tag.presentation.dto.response

import team.themoment.honeypotserver.domain.tag.domain.Tag

data class TagResponse(
    val id: Long,
    val name: String,
) {
    companion object {
        fun from(tag: Tag): TagResponse = TagResponse(id = tag.id, name = tag.name)
    }
}
