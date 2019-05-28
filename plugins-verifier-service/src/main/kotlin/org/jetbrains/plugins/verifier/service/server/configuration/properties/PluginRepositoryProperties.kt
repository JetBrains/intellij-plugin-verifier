package org.jetbrains.plugins.verifier.service.server.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URL

@ConfigurationProperties("verifier.service.plugins.repository")
class PluginRepositoryProperties {
  lateinit var url: URL
  lateinit var token: String
}