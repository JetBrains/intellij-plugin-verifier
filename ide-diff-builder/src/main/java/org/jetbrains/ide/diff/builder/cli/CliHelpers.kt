package org.jetbrains.ide.diff.builder.cli

/**
 * Prints the specified [message] to standard error
 * and exits the program with non-zero code.
 */
fun exit(message: String): Nothing {
  System.err.println(message)
  System.exit(1)
  throw RuntimeException()
}