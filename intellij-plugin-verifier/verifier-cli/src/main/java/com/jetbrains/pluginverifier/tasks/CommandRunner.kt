/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository

abstract class CommandRunner {

  abstract val commandName: String

  abstract fun getParametersBuilder(
    pluginRepository: PluginRepository,
    ideFilesBank: IdeFilesBank,
    pluginDetailsCache: PluginDetailsCache,
    reportage: PluginVerificationReportage
  ): TaskParametersBuilder

  abstract fun createTask(parameters: TaskParameters): Task

}