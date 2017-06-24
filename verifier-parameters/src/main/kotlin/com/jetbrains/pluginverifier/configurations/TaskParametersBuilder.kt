package com.jetbrains.pluginverifier.configurations

import com.jetbrains.pluginverifier.options.CmdOpts

interface TaskParametersBuilder<out Params : TaskParameters> {
  fun build(opts: CmdOpts, freeArgs: List<String>): Params
}