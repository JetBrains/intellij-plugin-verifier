package com.jetbrains.pluginverifier.client.commands

import com.jetbrains.pluginverifier.client.BaseCmdOpts

/**
 * @author Sergey Patrikeev
 */
interface Command {

  fun name(): String

  fun execute(opts: BaseCmdOpts, freeArgs: List<String>)
}