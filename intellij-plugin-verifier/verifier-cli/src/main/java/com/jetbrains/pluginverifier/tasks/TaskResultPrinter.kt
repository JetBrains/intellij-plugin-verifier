/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks

/**
 * Implementations of this interface print
 * the verification [results] [TaskResult]
 * in a way specific for a concrete [Task].
 */
interface TaskResultPrinter {
  fun printResults(taskResult: TaskResult)
}