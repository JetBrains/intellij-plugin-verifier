package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.options.CmdOpts

interface TaskParametersBuilder {
  fun build(opts: CmdOpts, freeArgs: List<String>): TaskParameters
}