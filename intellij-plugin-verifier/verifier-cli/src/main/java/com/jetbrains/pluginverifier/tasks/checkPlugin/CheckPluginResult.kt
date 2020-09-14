/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import com.jetbrains.pluginverifier.tasks.TaskResult

class CheckPluginResult(
  val invalidPluginFiles: List<InvalidPluginFile>,
  val results: List<PluginVerificationResult>
) : TaskResult {
  override fun createTaskResultsPrinter(pluginRepository: PluginRepository) =
    CheckPluginResultPrinter(pluginRepository)
}
