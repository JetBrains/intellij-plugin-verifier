/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.dependencies.resolution.createIdeBundledOrPluginRepositoryDependencyFinder
import com.jetbrains.pluginverifier.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.verifiers.filter.DynamicallyLoadedFilter
import com.jetbrains.pluginverifier.verifiers.packages.DefaultPackageFilter
import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator
import org.jetbrains.plugins.verifier.service.tasks.Task

/**
 * Task that performs [scheduledVerification].
 */
class VerifyPluginTask(
  private val scheduledVerification: ScheduledVerification,
  private val pluginDetailsCache: PluginDetailsCache,
  private val archiveManager: PluginArchiveManager,
  private val ideDescriptorsCache: IdeDescriptorsCache,
  private val pluginRepository: PluginRepository,
  private val problemsFilters: List<ProblemsFilter>
) : Task<PluginVerificationResult>("Check ${scheduledVerification.availableIde} against ${scheduledVerification.updateInfo}", "VerifyPlugin"),
  Comparable<VerifyPluginTask> {

  override fun execute(progress: ProgressIndicator): PluginVerificationResult {
    val cacheEntry = ideDescriptorsCache.getIdeDescriptorCacheEntry(scheduledVerification.availableIde.version)
    return cacheEntry.use {
      when (cacheEntry) {
        is IdeDescriptorsCache.Result.Found -> {
          val ideDescriptor = cacheEntry.ideDescriptor
          checkPluginWithIde(ideDescriptor)
        }
        is IdeDescriptorsCache.Result.NotFound -> {
          throw IllegalStateException("IDE ${scheduledVerification.availableIde} is not found: " + cacheEntry.reason)
        }
        is IdeDescriptorsCache.Result.Failed -> {
          throw IllegalStateException("Failed to get ${scheduledVerification.availableIde}: ${cacheEntry.reason}", cacheEntry.error)
        }
      }
    }
  }

  private fun checkPluginWithIde(ideDescriptor: IdeDescriptor): PluginVerificationResult {
    val dependencyFinder = createIdeBundledOrPluginRepositoryDependencyFinder(
      ideDescriptor.ide,
      pluginRepository,
      pluginDetailsCache
    )

    val classResolverProvider = DefaultClassResolverProvider(
      dependencyFinder,
      ideDescriptor,
      DefaultPackageFilter(emptyList()),
      archiveManager = archiveManager
    )
    val verificationDescriptor = PluginVerificationDescriptor.IDE(ideDescriptor, classResolverProvider, scheduledVerification.updateInfo)
    return PluginVerifier(
      verificationDescriptor,
      problemsFilters,
      pluginDetailsCache,
      listOf(DynamicallyLoadedFilter()),
      false
    ).loadPluginAndVerify()
  }

  /**
   * Comparison result is used by the task manager
   * to order tasks execution.
   *
   * 1) Newer plugins first.
   * 2) Manually scheduled verifications first.
   */
  override fun compareTo(other: VerifyPluginTask): Int {
    val updateIdCmp = other.scheduledVerification.updateInfo.updateId.compareTo(scheduledVerification.updateInfo.updateId)
    if (scheduledVerification.manually && other.scheduledVerification.manually) {
      return updateIdCmp
    }
    /**
     * Manually scheduled verification has priority.
     */
    if (scheduledVerification.manually) {
      return -1
    }
    if (other.scheduledVerification.manually) {
      return 1
    }
    return updateIdCmp
  }

}