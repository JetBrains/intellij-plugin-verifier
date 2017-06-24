package com.jetbrains.pluginverifier.configurations

import com.jetbrains.pluginverifier.options.CmdOpts

interface ConfigurationParamsBuilder<out Params : ConfigurationParams> {
  fun build(opts: CmdOpts, freeArgs: List<String>): Params
}