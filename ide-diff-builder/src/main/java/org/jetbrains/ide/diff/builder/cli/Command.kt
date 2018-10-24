package org.jetbrains.ide.diff.builder.cli

/**
 * Base interface of all CLI commands of this tool.
 */
interface Command {
  val commandName: String

  val help: String

  fun execute(freeArgs: List<String>)
}