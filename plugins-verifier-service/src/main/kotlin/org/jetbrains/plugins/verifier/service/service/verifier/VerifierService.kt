/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.plugin.structure.base.utils.pluralizeWithNumber
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.filtering.IgnoredProblemsFilter
import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.network.ServerUnavailable503Exception
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.PluginRepository
import org.jetbrains.plugins.verifier.service.server.ServiceDAO
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.tasks.TaskDescriptor
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Verifier service is responsible for plugins bytecode verification using the *plugin verifier* tool.
 *
 * This service periodically accesses the plugin repository, fetches plugins which should be verified,
 * and sends the verification reports.
 *
 * [Plugin verifier integration with JetBrains Marketplace](https://confluence.jetbrains.com/display/PLREP/plugin-verifier+integration+with+the+plugins.jetbrains.com)
 */
class VerifierService(
  taskManager: TaskManager,
  private val verifierServiceProtocol: VerifierServiceProtocol,
  private val pluginDetailsCache: PluginDetailsCache,
  private val ideDescriptorsCache: IdeDescriptorsCache,
  private val verificationResultsFilter: VerificationResultFilter,
  private val pluginRepository: PluginRepository,
  private val serviceDAO: ServiceDAO,
  private val minIdeVersion: IdeVersion,
  period: Long
) : BaseService("VerifierService", 0, period, TimeUnit.SECONDS, taskManager) {

  private val scheduledVerifications = linkedMapOf<ScheduledVerification, TaskDescriptor>()

  private val lastVerifiedDate = hashMapOf<ScheduledVerification, Instant>()

  override fun doServe() {
    val allScheduledVerifications = try {
      verifierServiceProtocol.requestScheduledVerifications()
    } catch (e: ServerUnavailable503Exception) {
      logger.info("Do not schedule new verifications and pause the service because JetBrains Marketplace is currently unavailable")
      pauseVerification()
      return
    }

    val now = Instant.now()
    synchronized(this) {
      val verifications = allScheduledVerifications
        .filter { it.shouldVerify(now) }
        .sortedByDescending { it.updateInfo.updateId }
      logger.info("There are ${verifications.size} pending verifications")
      verifications.forEach { scheduleVerification(it, now) }
    }
  }

  private fun ScheduledVerification.shouldVerify(now: Instant) =
    this !in scheduledVerifications
      && !isCheckedRecently(this, now)
      && (manually || availableIde.version >= minIdeVersion)
      && verificationResultsFilter.shouldStartVerification(this, now)

  private fun isCheckedRecently(scheduledVerification: ScheduledVerification, now: Instant): Boolean {
    val lastTime = lastVerifiedDate[scheduledVerification] ?: Instant.EPOCH
    return lastTime.plus(Duration.of(10, ChronoUnit.MINUTES)).isAfter(now)
  }

  private fun scheduleVerification(scheduledVerification: ScheduledVerification, now: Instant) {
    lastVerifiedDate[scheduledVerification] = now

    val ignoreConditions = serviceDAO.ignoreConditions.toList()
    val ignoredProblemsFilter = IgnoredProblemsFilter(ignoreConditions)
    val ignoreProblemsFilters = listOf(ignoredProblemsFilter)

    val task = VerifyPluginTask(
      scheduledVerification,
      pluginDetailsCache,
      ideDescriptorsCache,
      pluginRepository,
      ignoreProblemsFilters
    )

    val taskDescriptor = taskManager.enqueue(
      task,
      { taskResult, taskDescriptor -> taskResult.onSuccess(taskDescriptor, scheduledVerification) },
      { error, _ -> onError(scheduledVerification, error) },
      { onCompletion(scheduledVerification) }
    )
    logger.info("Schedule verification $scheduledVerification with task #${taskDescriptor.taskId}")
    scheduledVerifications[scheduledVerification] = taskDescriptor
  }

  @Synchronized
  private fun onCompletion(scheduledVerification: ScheduledVerification) {
    scheduledVerifications.remove(scheduledVerification)
  }

  @Synchronized
  private fun onError(scheduledVerification: ScheduledVerification, error: Throwable) {
    logger.error("Verification failed $scheduledVerification", error)
  }

  /**
   * Temporarily pause verifications because JetBrains Marketplace
   * cannot process its results at the moment.
   *
   * Cancel all the scheduled verifications in order to avoid unnecessary work.
   */
  @Synchronized
  private fun pauseVerification() {
    for (taskDescriptor in scheduledVerifications.values) {
      taskManager.cancel(taskDescriptor)
    }
    logger.info("Cancelled " + "verification".pluralizeWithNumber(scheduledVerifications.size) + ": " + scheduledVerifications.keys.take(10).joinToString() + "...")
    scheduledVerifications.clear()
  }

  //Do not synchronize: results sending is performed from background threads.
  private fun PluginVerificationResult.onSuccess(taskDescriptor: TaskDescriptor, scheduledVerification: ScheduledVerification) {
    logger.info("Finished verification $scheduledVerification: $verificationVerdict")
    if (verificationResultsFilter.shouldSendVerificationResult(this, taskDescriptor.endTime!!, scheduledVerification)) {
      try {
        verifierServiceProtocol.sendVerificationResult(scheduledVerification, this)
        logger.info("Verification result has been successfully sent for $scheduledVerification")
      } catch (e: ServerUnavailable503Exception) {
        logger.info(
          "JetBrains Marketplace $pluginRepository is currently unavailable (HTTP 503). " +
            "Stop all the scheduled verification tasks."
        )
        pauseVerification()
      } catch (e: Exception) {
        e.rethrowIfInterrupted()
        logger.error("Unable to send verification result for $plugin", e)
      }
    } else {
      logger.info("Verification result for $plugin against $verificationTarget has been ignored")
    }
  }

  override fun onStop() = Unit
}
