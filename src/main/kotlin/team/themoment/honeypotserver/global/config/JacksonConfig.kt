package team.themoment.honeypotserver.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {
    @Bean
    @ConditionalOnMissingBean(ObjectMapper::class)
    fun objectMapper(): ObjectMapper =
        ObjectMapper()
            .registerModule(kotlinModule())
            .registerModule(JavaTimeModule())
}
