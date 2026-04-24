/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.intellij.plugin.ModuleLoadingRule

object ContentModuleFixture {
  fun content(build: ContentDsl.() -> Unit): String {
    val dsl = ContentDsl()
    dsl.build()
    return dsl.toXml()
  }

  class ContentDsl {
    private val modules = mutableListOf<PluginContentModuleDsl>()

    fun module(name: String) {
      modules += PluginContentModuleDsl(name, ModuleLoadingRule.OPTIONAL)
    }

    fun embeddedModule(name: String) {
      modules += PluginContentModuleDsl(name, ModuleLoadingRule.EMBEDDED)
    }

    internal fun toXml(): String =
      buildString {
        append("<content>\n")
        modules.forEach { module ->
          append("<module name=\"")
          append(module.name)
          append("\"")
          if (module.loadingRule == ModuleLoadingRule.EMBEDDED) {
            append(" loading=\"embedded\"")
          }
          append(" />\n")
        }
        append("</content>")
      }
  }

  private data class PluginContentModuleDsl(val name: String, val loadingRule: ModuleLoadingRule)

}
