package com.jetbrains.pluginverifier.tasks

import com.jetbrains.pluginverifier.options.CmdOpts

/**
 * [Builds] [build] the [TaskParameters] of the upcoming verification
 * by provided [CmdOpts] and a list of CLI arguments.
 */
interface TaskParametersBuilder {
  fun build(opts: CmdOpts, freeArgs: List<String>): TaskParameters
}