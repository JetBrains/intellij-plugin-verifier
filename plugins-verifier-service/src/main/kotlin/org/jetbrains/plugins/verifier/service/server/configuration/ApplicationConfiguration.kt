package org.jetbrains.plugins.verifier.service.server.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class ApplicationConfiguration : WebMvcConfigurer {
  override fun configurePathMatch(configurer: PathMatchConfigurer) {
    configurer.isUseTrailingSlashMatch = true
  }
}