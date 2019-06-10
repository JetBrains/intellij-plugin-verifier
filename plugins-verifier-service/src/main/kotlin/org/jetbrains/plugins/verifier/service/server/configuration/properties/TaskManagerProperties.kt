package org.jetbrains.plugins.verifier.service.server.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("verifier.service.task.manager")
class TaskManagerProperties {
  var concurrency: Int? = null
}