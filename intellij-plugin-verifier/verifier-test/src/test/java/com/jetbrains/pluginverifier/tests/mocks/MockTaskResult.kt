package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.TaskResult

class MockTaskResult : TaskResult {
  override fun createTaskResultsPrinter(pluginRepository: PluginRepository) = SimpleTaskResultPrinter()
}