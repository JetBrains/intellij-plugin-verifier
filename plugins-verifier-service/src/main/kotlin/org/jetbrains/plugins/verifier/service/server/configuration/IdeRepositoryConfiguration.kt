package org.jetbrains.plugins.verifier.service.server.configuration

import com.jetbrains.pluginverifier.ide.repositories.AndroidStudioIdeRepository
import com.jetbrains.pluginverifier.ide.repositories.CombinedIdeRepository
import com.jetbrains.pluginverifier.ide.repositories.IdeRepository
import com.jetbrains.pluginverifier.ide.repositories.ReleaseIdeRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class IdeRepositoryConfiguration {
  @Bean
  fun ideRepository(): IdeRepository = CombinedIdeRepository(listOf(ReleaseIdeRepository(), AndroidStudioIdeRepository()))
}