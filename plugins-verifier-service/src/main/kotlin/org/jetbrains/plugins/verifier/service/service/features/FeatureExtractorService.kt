/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.service.features

import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.ide.repositories.IdeRepository
import com.jetbrains.pluginverifier.network.NonSuccessfulResponseException
import com.jetbrains.pluginverifier.network.ServerUnavailable503Exception
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.tasks.TaskDescriptor
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import java.util.concurrent.TimeUnit

/**
 * Feature service is responsible for extracting the plugin features
 * using the _intellij-feature-extractor_ tool.
 *
 * This service periodically accesses the plugin repository, fetches plugins of which features should be extracted,
 * and sends the features' reports.
 */
class FeatureExtractorService(
  taskManager: TaskManager,
  private val featureServiceProtocol: FeatureServiceProtocol,
  private val ideDescriptorsCache: IdeDescriptorsCache,
  private val pluginDetailsCache: PluginDetailsCache,
  private val ideRepository: IdeRepository,
  private val featureExtractorIdeVersion: IdeVersion
) : BaseService("FeatureService", 0, 5, TimeUnit.MINUTES, taskManager) {
  private val scheduledUpdates = linkedMapOf<UpdateInfo, TaskDescriptor>()

  override fun doServe() {
    val updatesToExtract = try {
      featureServiceProtocol.getUpdatesToExtract()
    } catch (e: ServerUnavailable503Exception) {
      logger.info("JetBrains Marketplace ${e.serverUrl} is currently unavailable (HTTP 503)")
      return
    }

    synchronized(this) {
      logger.info("Extracting features of ${updatesToExtract.size} updates")
      for (updateInfo in updatesToExtract) {
        if (updateInfo !in scheduledUpdates) {
          schedule(updateInfo)
        }
      }
    }
  }

  private fun schedule(updateInfo: UpdateInfo) {
    val extractTask = ExtractFeaturesTask(
      updateInfo,
      ideDescriptorsCache,
      pluginDetailsCache,
      ideRepository,
      featureExtractorIdeVersion
    )
    val taskDescriptor = taskManager.enqueue(
      extractTask,
      { result, _ -> onSuccess(result) },
      { t, tid -> onError(t, tid, extractTask) },
      { _ -> onCompletion(extractTask) }
    )
    scheduledUpdates[updateInfo] = taskDescriptor
    logger.info("Schedule extraction of features for $updateInfo with taskId #${taskDescriptor.taskId}")
  }

  @Synchronized
  private fun onCompletion(task: ExtractFeaturesTask) {
    scheduledUpdates.remove(task.updateInfo)
  }

  @Synchronized
  private fun onError(
    error: Throwable,
    taskDescriptor: TaskDescriptor,
    task: ExtractFeaturesTask
  ) {
    logger.error("Unable to extract features of ${task.updateInfo} (#${taskDescriptor.taskId})", error)
  }

  @Synchronized
  private fun pauseFeaturesExtraction() {
    for ((update, taskDescriptor) in scheduledUpdates.entries) {
      logger.info("Cancel extraction of features for $update")
      taskManager.cancel(taskDescriptor)
    }
    scheduledUpdates.clear()
  }

  //Do not synchronize: results sending is performed from background threads.
  private fun onSuccess(result: ExtractFeaturesTask.Result) {
    with(result) {
      logger.info("Plugin $updateInfo is processed: $result")
      try {
        featureServiceProtocol.sendExtractedFeatures(this)
        logger.info("Features of $updateInfo have been successfully sent to JetBrains Marketplace.")
      } catch (e: ServerUnavailable503Exception) {
        logger.info("JetBrains Marketplace ${e.serverUrl} is currently unavailable. Stop all the scheduled updates.")
        pauseFeaturesExtraction()
      } catch (e: Exception) {
        e.rethrowIfInterrupted()
        //TODO: remove this check when JetBrains Marketplace is fixed.
        if (e is NonSuccessfulResponseException && e.responseCode == 409) {
          logger.info("JetBrains Marketplace is still responding HTTP 409: Conflict on attempt to send update results")
        } else {
          logger.error("Failed to send features result for $updateInfo", e)
        }
      }
    }
  }


}
