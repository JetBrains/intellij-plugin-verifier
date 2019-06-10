package org.jetbrains.plugins.verifier.service.server.configuration

import org.jetbrains.plugins.verifier.service.server.configuration.properties.TaskManagerProperties
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import org.jetbrains.plugins.verifier.service.tasks.TaskManagerImpl
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(TaskManagerProperties::class)
class TaskManagerConfiguration(private val taskManagerProperties: TaskManagerProperties) {
  @Bean
  fun taskManager(): TaskManager = TaskManagerImpl(taskManagerProperties.concurrency!!)
}