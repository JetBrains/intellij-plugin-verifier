package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.options.CmdOpts

interface TaskParametersBuilder {
  //todo: build of parameters might take much time so we should log the progress of its separate steps
  fun build(opts: CmdOpts, freeArgs: List<String>): TaskParameters
}