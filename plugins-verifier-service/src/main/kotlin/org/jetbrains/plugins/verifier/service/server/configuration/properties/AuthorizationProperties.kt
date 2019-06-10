package org.jetbrains.plugins.verifier.service.server.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("verifier.service.admin")
class AuthorizationProperties {
  lateinit var password: String
}