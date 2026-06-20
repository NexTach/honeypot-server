package team.themoment.honeypotserver.domain.gif.infra

import org.springframework.data.jpa.repository.JpaRepository
import team.themoment.honeypotserver.domain.gif.domain.Gif
import team.themoment.honeypotserver.domain.gif.domain.GifRepository

interface GifJpaRepository :
    JpaRepository<Gif, Long>,
    GifRepository
