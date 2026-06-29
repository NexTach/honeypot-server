package team.themoment.honeypotserver.global.security

import org.springframework.core.MethodParameter
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class CurrentUserArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentUser::class.java) &&
            parameter.parameterType == AuthPrincipal::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): AuthPrincipal? {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication?.principal as? AuthPrincipal
        if (principal != null) {
            return principal
        }

        // permitAll 경로(예: 공개 GIF `/raw`)는 익명 요청이 들어올 수 있다.
        // 파라미터가 Kotlin nullable(`AuthPrincipal?`)이면 null 을 허용하고,
        // 아니면 기존대로 인증 누락을 예외로 알린다(다른 보호 경로는 후방호환).
        if (parameter.isOptional) {
            return null
        }
        throw AuthenticationCredentialsNotFoundException("No authenticated user found in SecurityContext")
    }
}
