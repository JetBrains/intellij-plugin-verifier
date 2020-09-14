/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.repository.PluginRepository

/**
 * Base class of all the verification [tasks] [Task]' results.
 */
interface TaskResult {
  fun createTaskResultsPrinter(pluginRepository: PluginRepository): TaskResultPrinter
}