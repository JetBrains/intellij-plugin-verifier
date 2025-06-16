/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks

import com.jetbrains.plugin.structure.intellij.plugin.caches.PluginResourceCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository

interface CommandRunner {

  val commandName: String

  fun getParametersBuilder(
    pluginRepository: PluginRepository,
    pluginDetailsCache: PluginDetailsCache,
    extractedPluginCache: PluginResourceCache,
    reportage: PluginVerificationReportage
  ): TaskParametersBuilder

}