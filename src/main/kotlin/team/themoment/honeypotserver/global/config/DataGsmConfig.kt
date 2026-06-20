package team.themoment.honeypotserver.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import team.themoment.datagsm.sdk.oauth.DataGsmOAuthClient

@Configuration
class DataGsmConfig(
    private val dataGsmProperties: DataGsmProperties,
) {
    @Bean
    fun dataGsmOAuthClient(): DataGsmOAuthClient =
        DataGsmOAuthClient
            .builder(dataGsmProperties.clientId, dataGsmProperties.clientSecret)
            .authorizationBaseUrl(dataGsmProperties.authorizationBaseUrl)
            .userInfoBaseUrl(dataGsmProperties.userInfoBaseUrl)
            .build()
}
