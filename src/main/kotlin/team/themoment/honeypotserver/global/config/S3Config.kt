package team.themoment.honeypotserver.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

@Configuration
class S3Config(
    private val s3Properties: S3Properties,
) {

    private fun credentialsProvider(): StaticCredentialsProvider =
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create(s3Properties.accessKey, s3Properties.secretKey)
        )

    @Bean
    fun s3Client(): S3Client =
        S3Client.builder()
            .endpointOverride(URI.create(s3Properties.endpoint))
            .region(Region.US_EAST_1)
            .credentialsProvider(credentialsProvider())
            .forcePathStyle(true)
            .build()

    @Bean
    fun s3Presigner(): S3Presigner =
        S3Presigner.builder()
            .endpointOverride(URI.create(s3Properties.endpoint))
            .region(Region.US_EAST_1)
            .credentialsProvider(credentialsProvider())
            .build()
}
