package team.themoment.honeypotserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import team.themoment.honeypotserver.global.config.DataGsmProperties
import team.themoment.honeypotserver.global.config.S3Properties
import team.themoment.honeypotserver.global.security.JwtProperties

@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties(
    JwtProperties::class,
    S3Properties::class,
    DataGsmProperties::class,
)
class HoneypotServerApplication

fun main(args: Array<String>) {
    runApplication<HoneypotServerApplication>(*args)
}
