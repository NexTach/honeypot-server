package team.themoment.honeypotserver.domain.user.infra

import org.springframework.stereotype.Component
import team.themoment.datagsm.sdk.oauth.DataGsmOAuthClient
import team.themoment.datagsm.sdk.oauth.model.UserInfo
import team.themoment.honeypotserver.global.config.DataGsmProperties

@Component
class DataGsmClient(
    private val dataGsmOAuthClient: DataGsmOAuthClient,
    private val dataGsmProperties: DataGsmProperties,
) {
    fun buildLoginUrl(
        state: String,
        codeVerifier: String,
    ): String {
        val builder =
            dataGsmOAuthClient
                .createAuthorizationUrl(dataGsmProperties.redirectUri)
                .state(state)
                .scope("datagsm:self_read")
                .enablePkce(codeVerifier, "S256")
        return builder.build()
    }

    fun exchangeCodeForToken(
        code: String,
        codeVerifier: String,
    ): String {
        val tokenResponse =
            dataGsmOAuthClient.exchangeCodeForToken(
                code,
                dataGsmProperties.redirectUri,
                codeVerifier,
            )
        return tokenResponse.accessToken
    }

    fun getUserInfo(accessToken: String): UserInfo = dataGsmOAuthClient.getUserInfo(accessToken)
}
