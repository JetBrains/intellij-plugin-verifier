package org.jetbrains.plugins.verifier.service.api

import com.jetbrains.pluginverifier.configurations.CheckIdeResults
import com.jetbrains.pluginverifier.configurations.CheckPluginResults
import com.jetbrains.pluginverifier.configurations.CheckRangeResults
import com.jetbrains.pluginverifier.configurations.CheckTrunkApiResults
import org.jetbrains.plugins.verifier.service.client.waitCompletion

class GetResult(host: String, val taskId: Int, val command: String) : VerifierServiceApi<Any>(host) {
  override fun executeImpl(): Any {
    val taskId = TaskId(taskId)
    return when (command) {
      "check-ide" -> waitCompletion<CheckIdeResults>(service, taskId)
      "check-plugin" -> waitCompletion<CheckPluginResults>(service, taskId)
      "check-since-until" -> waitCompletion<CheckRangeResults>(service, taskId)
      "check-trunk-api" -> waitCompletion<CheckTrunkApiResults>(service, taskId)
      else -> {
        throw IllegalArgumentException("unknown command")
      }
    }
  }
}