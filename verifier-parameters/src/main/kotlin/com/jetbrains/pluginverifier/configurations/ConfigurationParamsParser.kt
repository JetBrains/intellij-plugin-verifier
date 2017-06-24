package com.jetbrains.pluginverifier.configurations

import com.jetbrains.pluginverifier.options.CmdOpts

interface ConfigurationParamsParser<out P : ConfigurationParams> {
  fun parse(opts: CmdOpts, freeArgs: List<String>): P
}