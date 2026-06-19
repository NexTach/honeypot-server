package team.themoment.honeypotserver.domain.gif.infra

import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import team.themoment.honeypotserver.global.config.S3Properties
import java.util.UUID

@Component
class GifStorageAdapter(
    private val s3Client: S3Client,
    private val s3Properties: S3Properties,
) {

    fun generateKey(): String = "gifs/${UUID.randomUUID()}.gif"

    fun upload(objectKey: String, bytes: ByteArray, contentType: String) {
        val putRequest = PutObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(objectKey)
            .contentType(contentType)
            .contentLength(bytes.size.toLong())
            .build()

        s3Client.putObject(putRequest, RequestBody.fromBytes(bytes))
    }

    fun delete(objectKey: String) {
        val deleteRequest = DeleteObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(objectKey)
            .build()

        s3Client.deleteObject(deleteRequest)
    }
}
