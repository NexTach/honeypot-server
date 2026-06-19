package team.themoment.honeypotserver.domain.gif.presentation.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class GifUploadRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val title: String,

    val description: String? = null,

    val isPublic: Boolean = true,

    val tags: List<String> = emptyList(),
)
