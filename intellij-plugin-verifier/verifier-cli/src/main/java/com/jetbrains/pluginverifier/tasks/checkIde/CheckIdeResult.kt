/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.tasks.TaskResult

class CheckIdeResult(
  val ide: PluginVerificationTarget.IDE,
  val results: List<PluginVerificationResult>,
  val missingCompatibleVersionsProblems: List<MissingCompatibleVersionProblem>
) : TaskResult()
