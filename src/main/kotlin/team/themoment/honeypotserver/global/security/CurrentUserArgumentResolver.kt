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
    ): AuthPrincipal {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal as? AuthPrincipal
            ?: throw AuthenticationCredentialsNotFoundException("No authenticated user found in SecurityContext")
    }
}
