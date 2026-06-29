package team.themoment.honeypotserver.domain.gif.infra

import org.springframework.stereotype.Component
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import team.themoment.honeypotserver.global.config.S3Properties
import java.util.UUID

@Component
class GifStorageAdapter(
    private val s3Client: S3Client,
    private val s3Properties: S3Properties,
) {
    fun generateKey(): String = "gifs/${UUID.randomUUID()}.gif"

    fun upload(
        objectKey: String,
        bytes: ByteArray,
        contentType: String,
    ) {
        val putRequest =
            PutObjectRequest
                .builder()
                .bucket(s3Properties.bucket)
                .key(objectKey)
                .contentType(contentType)
                .contentLength(bytes.size.toLong())
                .build()

        s3Client.putObject(putRequest, RequestBody.fromBytes(bytes))
    }

    /**
     * 객체를 스트림으로 다운로드한다. 미디어 서빙(`/raw`)에서 백엔드가 바이트를 중계할 때 사용.
     * 호출자가 반환된 스트림을 반드시 close 해야 한다(`use {}`).
     */
    fun download(objectKey: String): ResponseInputStream<GetObjectResponse> {
        val getRequest =
            GetObjectRequest
                .builder()
                .bucket(s3Properties.bucket)
                .key(objectKey)
                .build()

        return s3Client.getObject(getRequest)
    }

    fun delete(objectKey: String) {
        val deleteRequest =
            DeleteObjectRequest
                .builder()
                .bucket(s3Properties.bucket)
                .key(objectKey)
                .build()

        s3Client.deleteObject(deleteRequest)
    }
}
