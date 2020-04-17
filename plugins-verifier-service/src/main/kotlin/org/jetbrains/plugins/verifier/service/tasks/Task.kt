/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.tasks

typealias TaskType = String

/**
 * Service task to be [execute]d.
 */
abstract class Task<out T>(
  /**
   * Presentable name of the to be displayed
   * to users.
   */
  val presentableName: String,

  /**
   * Type used to distinguish tasks
   * and run independent tasks in parallel.
   */
  val taskType: TaskType
) {

  /**
   * Executes the task and returns the result.
   *
   * [progress] can be used to track execution progress and state.
   *
   * @throws InterruptedException if the current thread has been interrupted while executing the task
   */
  @Throws(InterruptedException::class)
  abstract fun execute(progress: ProgressIndicator): T

  final override fun toString() = presentableName

}