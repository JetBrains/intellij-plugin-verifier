package org.jetbrains.ide.diff.builder.cli

import kotlin.system.exitProcess

/**
 * Prints the specified [message] to standard error
 * and exits the program with non-zero code.
 */
fun exit(message: String): Nothing {
  System.err.println(message)
  exitProcess(1)
}