/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.checkPluginApi

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.tasks.TaskParameters

class CheckPluginApiParams(
  private val basePluginDetails: PluginDetails,
  private val newPluginDetails: PluginDetails,
  private val jdkDescriptor: JdkDescriptor,
  val problemsFilters: List<ProblemsFilter>,
  val baseVerificationDescriptors: List<PluginVerificationDescriptor.Plugin>,
  val newVerificationDescriptors: List<PluginVerificationDescriptor.Plugin>,
  val baseVerificationTarget: PluginVerificationTarget.Plugin,
  val newVerificationTarget: PluginVerificationTarget.Plugin,
  val excludeExternalBuildClassesSelector: Boolean
) : TaskParameters {

  override val presentableText
    get() = buildString {
      appendLine("Base verifications (${baseVerificationDescriptors.size}): ")
      for ((apiPlugin, pluginVerifications) in baseVerificationDescriptors.groupBy { it.apiPlugin }) {
        appendLine(apiPlugin.presentableName + " against " + pluginVerifications.joinToString { it.checkedPlugin.presentableName })
      }

      appendLine("New verifications: (${newVerificationDescriptors.size}): ")
      for ((apiPlugin, pluginVerifications) in newVerificationDescriptors.groupBy { it.apiPlugin }) {
        appendLine(apiPlugin.presentableName + " against " + pluginVerifications.joinToString { it.checkedPlugin.presentableName })
      }
    }

  override fun createTask() = CheckPluginApiTask(this)

  override fun close() {
    basePluginDetails.closeLogged()
    newPluginDetails.closeLogged()
    jdkDescriptor.closeLogged()
  }

}