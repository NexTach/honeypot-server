package team.themoment.honeypotserver.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "datagsm.oauth")
data class DataGsmProperties(
    val clientId: String,
    val clientSecret: String,
    val authorizationBaseUrl: String,
    val userInfoBaseUrl: String,
    val redirectUri: String,
)
