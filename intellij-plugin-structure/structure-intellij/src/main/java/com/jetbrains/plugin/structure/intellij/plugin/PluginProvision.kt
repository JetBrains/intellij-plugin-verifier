package com.jetbrains.plugin.structure.intellij.plugin

/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
sealed class PluginProvision {
  enum class Source(val description: String) {
    ID("ID"),
    NAME("name"),
    ALIAS("plugin alias"),
    CONTENT_MODULE_ID("content module ID");

    override fun toString() = description
  }

  class Found(val plugin: IdePlugin, val source: Source) : PluginProvision() {
    override fun toString(): String {
      return "Found via ${source.description}"
    }
  }
  object NotFound : PluginProvision()
}
