package org.jetbrains.plugins.verifier.service.service

import org.jetbrains.plugins.verifier.service.service.featureExtractor.FeatureService
import org.jetbrains.plugins.verifier.service.service.ide.IdeListUpdater
import org.jetbrains.plugins.verifier.service.service.verifier.VerifierService
import org.jetbrains.plugins.verifier.service.tasks.TaskManager

/**
 * @author Sergey Patrikeev
 */
object ServerInstance {
  val taskManager = TaskManager(100)

  val verifierService = VerifierService(taskManager)

  val ideListUpdater = IdeListUpdater(taskManager)

  val featureService = FeatureService(taskManager)

  fun stop() {
    taskManager.stop()
    ideListUpdater.stop()
    verifierService.stop()
    featureService.stop()
  }

}