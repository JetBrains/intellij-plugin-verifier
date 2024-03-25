/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import java.nio.file.Path

/**
 * Descriptor of a plugin which was specified for the verification
 * but which has structure [problems] [problems].
 */
data class InvalidPluginFile(val file: Path, val problems: List<PluginProblem>)