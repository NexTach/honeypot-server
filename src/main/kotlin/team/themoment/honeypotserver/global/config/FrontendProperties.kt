package team.themoment.honeypotserver.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 프론트엔드 연동 설정. 크로스 플랫폼 대응을 위해 origin 과 콜백 redirect-uri 를 다중으로 받는다.
 * 환경변수는 콤마 구분 문자열(`http://a,http://b`)로 주입하면 List 로 바인딩된다.
 */
@ConfigurationProperties(prefix = "honeypot.frontend")
data class FrontendProperties(
    val origins: List<String>,
    val redirectUris: List<String>,
) {
    /** 프론트가 redirect_uri 를 지정하지 않거나 허용 목록에 없을 때 사용할 기본값. */
    val defaultRedirectUri: String
        get() = redirectUris.first()

    /**
     * 요청된 redirect_uri 가 허용 목록(정확 일치)에 있으면 그대로, 아니면 [defaultRedirectUri] 를 반환한다.
     * open redirect 방지를 위해 화이트리스트 밖의 값은 받아들이지 않는다.
     */
    fun resolveRedirectUri(requested: String?): String = requested?.takeIf { it in redirectUris } ?: defaultRedirectUri
}
