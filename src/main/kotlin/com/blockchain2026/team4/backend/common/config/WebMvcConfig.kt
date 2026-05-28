package com.blockchain2026.team4.backend.common.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Path

@Configuration
class WebMvcConfig(
    private val appProperties: AppProperties,
) : WebMvcConfigurer {
    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addRedirectViewController("/swagger-ui.html", "/swagger-ui/index.html")
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val directory = Path.of(appProperties.storage.imageDirectory).toAbsolutePath().normalize()
        // Spring requires resource locations to end with '/' to serve directory contents.
        registry.addResourceHandler("${appProperties.storage.publicUrlPrefix.trimEnd('/')}/**")
            .addResourceLocations("${directory.toUri()}/")
    }
}
