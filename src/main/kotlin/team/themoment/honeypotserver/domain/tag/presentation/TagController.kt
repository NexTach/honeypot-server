package team.themoment.honeypotserver.domain.tag.presentation

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import team.themoment.honeypotserver.domain.tag.application.TagService
import team.themoment.honeypotserver.domain.tag.presentation.dto.response.TagResponse

@RestController
@RequestMapping("/v1/tags")
class TagController(
    private val tagService: TagService,
) {

    @GetMapping
    fun searchTags(@RequestParam(defaultValue = "") keyword: String): List<TagResponse> {
        return tagService.searchByKeyword(keyword).map { TagResponse.from(it) }
    }
}
