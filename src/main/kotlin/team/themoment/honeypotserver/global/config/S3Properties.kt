package team.themoment.honeypotserver.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "honeypot.s3")
data class S3Properties(
    val endpoint: String,
    val accessKey: String,
    val secretKey: String,
    val bucket: String,
)
