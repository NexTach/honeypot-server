package team.themoment.honeypotserver.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "honeypot.s3")
data class S3Properties(
    val endpoint: String,
    val publicEndpoint: String?,
    val accessKey: String,
    val secretKey: String,
    val bucket: String,
    val presignTtl: Duration,
) {
    /**
     * presigned URL 생성에 사용할 외부 공개 endpoint.
     * 미설정(`null`/blank)이면 내부 [endpoint] 를 그대로 사용한다.
     * 배포 환경에서 앱→스토리지 업로드는 내부 endpoint, 브라우저 직접 접근(presigned)은 공개 endpoint 로 분리한다.
     */
    val effectivePublicEndpoint: String
        get() = publicEndpoint?.takeIf { it.isNotBlank() } ?: endpoint
}
