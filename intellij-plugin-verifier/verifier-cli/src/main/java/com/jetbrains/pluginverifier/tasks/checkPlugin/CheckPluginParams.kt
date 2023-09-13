/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.pluginverifier.PluginVerificationDescriptor
import com.jetbrains.pluginverifier.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import com.jetbrains.pluginverifier.tasks.TaskParameters

class CheckPluginParams(
  internal val ideDescriptors: List<IdeDescriptor>,
  val problemsFilters: List<ProblemsFilter>,
  val verificationDescriptors: List<PluginVerificationDescriptor>,
  val invalidPluginFiles: List<InvalidPluginFile>,
  val excludeExternalBuildClassesSelector: Boolean,
  val internalApiVerificationMode: InternalApiVerificationMode = InternalApiVerificationMode.FULL
) : TaskParameters {

  override val presentableText
    get() = buildString {
      appendLine("Scheduled verifications (${verificationDescriptors.size}):")
      appendLine(verificationDescriptors.joinToString())
    }

  override fun createTask() = CheckPluginTask(this)

  override fun close() {
    ideDescriptors.forEach { it.closeLogged() }
  }

}

enum class InternalApiVerificationMode {
  /**
   * Full verification of internal API usages for all plugins.
   */
  FULL,

  /**
   * Internal plugins that use internal APIs will not be reported.
   */
  IGNORE_IN_JETBRAINS_PLUGINS
}