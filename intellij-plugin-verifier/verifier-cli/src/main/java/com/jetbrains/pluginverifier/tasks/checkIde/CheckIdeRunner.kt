/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.CommandRunner
import com.jetbrains.pluginverifier.tasks.TaskParameters
import com.jetbrains.pluginverifier.tasks.TaskResultPrinter

/**
 * Runner of the ['check-ide'] [CheckIdeTask] command.
 */
class CheckIdeRunner : CommandRunner() {
  override val commandName: String = "check-ide"

  override fun getParametersBuilder(
    pluginRepository: PluginRepository,
    ideFilesBank: IdeFilesBank,
    pluginDetailsCache: PluginDetailsCache,
    reportage: PluginVerificationReportage
  ) = CheckIdeParamsBuilder(pluginRepository, pluginDetailsCache, reportage)

  override fun createTask(parameters: TaskParameters) = CheckIdeTask(parameters as CheckIdeParams)

  override fun createTaskResultsPrinter(pluginRepository: PluginRepository): TaskResultPrinter =
    CheckIdeResultPrinter(pluginRepository)

}