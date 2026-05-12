package com.blockchain2026.team4.backend.common.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Path

@Configuration
class WebMvcConfig(
    private val appProperties: AppProperties,
) : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val directory = Path.of(appProperties.storage.imageDirectory).toAbsolutePath().normalize()
        registry.addResourceHandler("${appProperties.storage.publicUrlPrefix.trimEnd('/')}/**")
            .addResourceLocations(directory.toUri().toString())
    }
}
