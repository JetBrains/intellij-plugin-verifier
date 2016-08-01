package com.jetbrains.pluginverifier.configurations

import com.jetbrains.pluginverifier.utils.CmdOpts

interface Params

interface ParamsParser {
  fun parse(opts: CmdOpts, freeArgs: List<String>): Params
}