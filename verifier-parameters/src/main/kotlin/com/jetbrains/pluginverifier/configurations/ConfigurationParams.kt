package com.jetbrains.pluginverifier.configurations

import com.jetbrains.pluginverifier.utils.CmdOpts
import java.io.Closeable

interface ConfigurationParams : Closeable {
  fun presentableText(): String
}

interface ConfigurationParamsParser {
  fun parse(opts: CmdOpts, freeArgs: List<String>): ConfigurationParams
}