/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.reporting.ignoring

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.writeText
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.common.CollectingReporter
import java.nio.file.Path

/**
 * Collects all [ProblemIgnoredEvent]s reported
 * for one plugin against one target
 * and saves them to `<plugin-verification>/ignored-problems.txt` file.
 */
class IgnoredProblemsReporter(
  private val pluginVerificationDirectory: Path,
  private val verificationTarget: PluginVerificationTarget
) : Reporter<ProblemIgnoredEvent> {

  private val collectingReporter = CollectingReporter<ProblemIgnoredEvent>()

  override fun report(t: ProblemIgnoredEvent) {
    collectingReporter.report(t)
  }

  override fun close() {
    try {
      saveIgnoredProblems()
    } finally {
      collectingReporter.closeLogged()
    }
  }

  private fun saveIgnoredProblems() {
    val allIgnoredProblems = collectingReporter.allReported
    if (allIgnoredProblems.isNotEmpty()) {
      val ignoredProblemsFile = pluginVerificationDirectory.resolve("ignored-problems.txt")
      ignoredProblemsFile.writeText(
        AllIgnoredProblemsReporter.formatManyIgnoredProblems(verificationTarget, allIgnoredProblems)
      )
    }
  }


}