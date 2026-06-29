package team.themoment.honeypotserver.domain.gif.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import team.themoment.honeypotserver.domain.gif.infra.GifStorageAdapter
import team.themoment.honeypotserver.global.security.AuthPrincipal

@Service
@Transactional(readOnly = true)
class GifMediaService(
    private val gifQueryService: GifQueryService,
    private val gifStorageAdapter: GifStorageAdapter,
) {
    /**
     * 가시성 인가 후 GIF 원본 바이트 스트림을 연다.
     * - 가시성 규칙은 [GifQueryService.checkVisibility] 재사용: admin 전체 허용,
     *   blinded·비공개 미인가는 404. 공개 GIF 는 익명(principal=null) 도 통과.
     * - 반환된 [AuthorizedGifStream.body] 는 호출자가 반드시 close 해야 한다.
     */
    fun openAuthorizedStream(
        gifId: Long,
        principal: AuthPrincipal?,
    ): AuthorizedGifStream {
        val gif = gifQueryService.getGifDetail(gifId, principal)
        val body = gifStorageAdapter.download(gif.objectKey)
        return AuthorizedGifStream(
            body = body,
            contentType = gif.contentType,
            contentLength = body.response().contentLength(),
            isPublic = gif.isPublic && !gif.blindedByAdmin,
        )
    }
}

/**
 * 인가된 GIF 미디어 스트림과 응답 헤더 구성에 필요한 메타데이터.
 * [body] 소비 후 호출자가 close 책임을 진다.
 */
data class AuthorizedGifStream(
    val body: ResponseInputStream<GetObjectResponse>,
    val contentType: String,
    val contentLength: Long,
    val isPublic: Boolean,
)
