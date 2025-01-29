/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

sealed class Module(open val name: String) {
  data class InlineModule(override val name: String, val textContent: String) : Module(name) {
    override fun toString(): String = "$name (CDATA module, ${textContent.length} characters)"
  }
  data class FileBasedModule(override val name: String, val configFile: String) : Module(name) {
    override fun toString(): String = "$name (file module, $configFile)"
  }
}
