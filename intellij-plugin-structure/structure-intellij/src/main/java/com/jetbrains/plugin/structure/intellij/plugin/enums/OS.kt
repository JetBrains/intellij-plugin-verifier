/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.enums

private const val OS_MODULE_PREFIX = "com.intellij.modules.os."

enum class OS(
  private val suffix: String,
  val parents: Set<OS> = emptySet() // used on the MP side to resolve compatibility
) {
  Windows("windows"),
  Unix("unix"),
  MacOS("macos", parents = setOf(Unix)),
  Linux("linux", parents = setOf(Unix)),
  FreeBSD("freebsd", parents = setOf(Unix));

  val pluginAlias = OS_MODULE_PREFIX + suffix

  companion object {
    fun getByModule(moduleName: String): OS? = values().find {
      it.pluginAlias.equals(moduleName, ignoreCase = true)
    }
  }
}