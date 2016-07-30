package org.jetbrains.plugins.verifier.service.api

import com.jetbrains.pluginverifier.configurations.CheckIdeResults
import com.jetbrains.pluginverifier.configurations.CheckPluginResults
import org.jetbrains.plugins.verifier.service.client.VerifierService
import org.jetbrains.plugins.verifier.service.client.waitCompletion
import org.jetbrains.plugins.verifier.service.results.CheckPluginAgainstSinceUntilBuildsResults
import org.jetbrains.plugins.verifier.service.results.CheckTrunkApiResults

class GetResult(val host: String, val taskId: Int, val command: String) : VerifierServiceApi<Any> {
  override fun execute(): Any {
    val service = VerifierService(host)
    val taskId = TaskId(taskId)
    return when (command) {
      "check-ide" -> waitCompletion<CheckIdeResults>(service, taskId)
      "check-plugin" -> waitCompletion<CheckPluginResults>(service, taskId)
      "check-since-until" -> waitCompletion<CheckPluginAgainstSinceUntilBuildsResults>(service, taskId)
      "check-trunk-api" -> waitCompletion<CheckTrunkApiResults>(service, taskId)
      else -> {
        throw IllegalArgumentException("unknown command")
      }
    }
  }
}