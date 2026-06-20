package team.themoment.honeypotserver.global.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import team.themoment.honeypotserver.global.security.CurrentUserArgumentResolver

@Configuration
class WebMvcConfig : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(CurrentUserArgumentResolver())
    }
}
