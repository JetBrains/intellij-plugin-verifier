/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import com.jetbrains.pluginverifier.tasks.TaskResult

/**
 * Result of the `check-plugin` task
 *
 * @param invalidPluginFiles list of plugins with structural problems or descriptor validation problems
 * @param results list of nonstructural problems of plugins
 * @param ideDescriptorsWithInvalidPlugins between specific IDE and corresponding list of plugins with structural problems.
 * This is used to map the verification target IDE to list of plugins that did not even enter the actual verification
 * stage due to the structural problems.
 *
 */
class CheckPluginResult(
  val invalidPluginFiles: List<InvalidPluginFile>,
  val results: List<PluginVerificationResult>,
  val ideDescriptorsWithInvalidPlugins: Map<IdeDescriptor, List<InvalidPluginFile>> = emptyMap()
) : TaskResult {
  override fun createTaskResultsPrinter(pluginRepository: PluginRepository) =
    CheckPluginResultPrinter(pluginRepository)
}
