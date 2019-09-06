package org.jetbrains.plugins.verifier.service.server.configuration

import com.jetbrains.pluginverifier.ide.repositories.IdeRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import org.jetbrains.plugins.verifier.service.server.configuration.properties.PluginRepositoryProperties
import org.jetbrains.plugins.verifier.service.service.features.DefaultFeatureServiceProtocol
import org.jetbrains.plugins.verifier.service.service.features.FeatureServiceProtocol
import org.jetbrains.plugins.verifier.service.service.ide.AvailableIdeProtocol
import org.jetbrains.plugins.verifier.service.service.ide.DefaultAvailableIdeProtocol
import org.jetbrains.plugins.verifier.service.service.verifier.DefaultVerifierServiceProtocol
import org.jetbrains.plugins.verifier.service.service.verifier.VerifierServiceProtocol
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@EnableConfigurationProperties(PluginRepositoryProperties::class)
@Configuration
class MarketplaceRepositoryConfiguration(private val pluginRepositoryProperties: PluginRepositoryProperties) {
  @Bean
  fun pluginRepository() = MarketplaceRepository(pluginRepositoryProperties.url)

  @Bean
  fun availableIdeProtocol(pluginRepository: MarketplaceRepository): AvailableIdeProtocol = DefaultAvailableIdeProtocol(
      pluginRepositoryProperties.token, pluginRepository
  )

  @Bean
  fun featureServiceProtocol(pluginRepository: MarketplaceRepository): FeatureServiceProtocol =
      DefaultFeatureServiceProtocol(pluginRepositoryProperties.token, pluginRepository)

  @Bean
  fun verifierServiceProtocol(pluginRepository: MarketplaceRepository, ideRepository: IdeRepository): VerifierServiceProtocol =
      DefaultVerifierServiceProtocol(pluginRepositoryProperties.token, pluginRepository, ideRepository)
}