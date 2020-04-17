/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.ide.diff.builder.cli

/**
 * Base interface of all CLI commands of this tool.
 */
interface Command {
  val commandName: String

  val help: String

  fun execute(freeArgs: List<String>)
}