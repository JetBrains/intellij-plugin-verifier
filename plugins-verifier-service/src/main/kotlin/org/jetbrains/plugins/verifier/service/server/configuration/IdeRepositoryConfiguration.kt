package org.jetbrains.plugins.verifier.service.server.configuration

import com.jetbrains.pluginverifier.ide.IdeRepository
import com.jetbrains.pluginverifier.ide.ReleaseIdeRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class IdeRepositoryConfiguration {
  @Bean
  fun ideRepository(): IdeRepository = ReleaseIdeRepository()
}