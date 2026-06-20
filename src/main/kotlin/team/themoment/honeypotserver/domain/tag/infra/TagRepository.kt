package team.themoment.honeypotserver.domain.tag.infra

import org.springframework.data.jpa.repository.JpaRepository
import team.themoment.honeypotserver.domain.tag.domain.Tag

interface TagRepository : JpaRepository<Tag, Long> {
    fun findByName(name: String): Tag?

    fun findByNameContainingIgnoreCase(keyword: String): List<Tag>
}
