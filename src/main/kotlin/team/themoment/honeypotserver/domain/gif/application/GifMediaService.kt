package team.themoment.honeypotserver.domain.gif.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import team.themoment.honeypotserver.domain.gif.domain.Gif
import team.themoment.honeypotserver.global.config.S3Properties
import team.themoment.honeypotserver.global.security.AuthPrincipal
import java.net.URI

@Service
@Transactional(readOnly = true)
class GifMediaService(
    private val gifQueryService: GifQueryService,
    private val s3Presigner: S3Presigner,
    private val s3Properties: S3Properties,
) {
    fun getPresignedUrl(
        gifId: Long,
        principal: AuthPrincipal?,
    ): URI {
        val gif = gifQueryService.getGifDetail(gifId, principal)
        return generatePresignedUrl(gif)
    }

    private fun generatePresignedUrl(gif: Gif): URI {
        val getObjectRequest =
            GetObjectRequest
                .builder()
                .bucket(s3Properties.bucket)
                .key(gif.objectKey)
                .build()

        val presignRequest =
            GetObjectPresignRequest
                .builder()
                .signatureDuration(s3Properties.presignTtl)
                .getObjectRequest(getObjectRequest)
                .build()

        val presignedRequest = s3Presigner.presignGetObject(presignRequest)
        return presignedRequest.url().toURI()
    }
}
