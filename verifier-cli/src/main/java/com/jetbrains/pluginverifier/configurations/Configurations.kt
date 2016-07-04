package com.jetbrains.pluginverifier.configurations

import com.jetbrains.pluginverifier.utils.Opts

interface Params

interface ParamsParser {
  fun parse(opts: Opts, freeArgs: List<String>): Params
}


interface Results

interface Configuration {
  fun execute(): Results
}
