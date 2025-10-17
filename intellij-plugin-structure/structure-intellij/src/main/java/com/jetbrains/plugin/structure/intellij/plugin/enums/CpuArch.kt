/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.enums

private const val ARCH_MODULE_PREFIX = "com.intellij.modules.arch."

enum class CpuArch(private val suffix: String) {
  X86("x86"),
  X86_64("x86_64"),
  ARM32("arm32"),
  ARM64("arm64");

  val pluginAlias = ARCH_MODULE_PREFIX + suffix

  companion object {
    fun getByModule(moduleName: String): CpuArch? = values().find {
      it.pluginAlias.equals(moduleName, ignoreCase = true)
    }
  }
}