package team.themoment.honeypotserver.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

@Configuration
class S3Config(
    private val s3Properties: S3Properties,
) {
    private fun credentialsProvider(): StaticCredentialsProvider =
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create(s3Properties.accessKey, s3Properties.secretKey),
        )

    @Bean
    fun s3Client(): S3Client =
        S3Client
            .builder()
            .endpointOverride(URI.create(s3Properties.endpoint))
            .region(Region.US_EAST_1)
            .credentialsProvider(credentialsProvider())
            .forcePathStyle(true)
            .build()

    // presigned URL 은 브라우저가 직접 접근하므로 외부 공개 endpoint 로 서명한다.
    // path-style 서명을 강제해 S3Client(forcePathStyle=true) 와 동일한 canonical URI 를 사용한다.
    @Bean
    fun s3Presigner(): S3Presigner =
        S3Presigner
            .builder()
            .endpointOverride(URI.create(s3Properties.effectivePublicEndpoint))
            .region(Region.US_EAST_1)
            .credentialsProvider(credentialsProvider())
            .serviceConfiguration(
                S3Configuration
                    .builder()
                    .pathStyleAccessEnabled(true)
                    .build(),
            ).build()
}
