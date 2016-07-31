package org.jetbrains.plugins.verifier.service.api

import org.jetbrains.plugins.verifier.service.client.executeSuccessfully
import org.slf4j.LoggerFactory

class CancelTask(host: String, val taskId: Int) : VerifierServiceApi<Unit>(host) {

  companion object {
    private val LOG = LoggerFactory.getLogger(CancelTask::class.java)
  }


  override fun executeImpl(): Unit {
    val call = service.taskResultsService.cancelTask(TaskId(taskId))
    val response = call.executeSuccessfully()
    LOG.info("Cancellation result: ${response.body().string()}")
  }
}