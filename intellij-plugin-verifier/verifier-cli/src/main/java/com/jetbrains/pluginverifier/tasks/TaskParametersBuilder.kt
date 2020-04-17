/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.options.CmdOpts

/**
 * [Builds] [build] the [TaskParameters] of the upcoming verification
 * by provided [CmdOpts] and a list of CLI arguments.
 */
interface TaskParametersBuilder {
  fun build(opts: CmdOpts, freeArgs: List<String>): TaskParameters
}