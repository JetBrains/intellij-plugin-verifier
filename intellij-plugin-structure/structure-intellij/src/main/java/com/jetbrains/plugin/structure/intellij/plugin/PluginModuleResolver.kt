/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.beans.PluginBean

/**
 * Resolves Plugin V2 Model `<content>/<module>` elements.
 */
internal class PluginModuleResolver {
  fun resolvePluginModules(pluginBean: PluginBean): List<Module> {
    val modules = pluginBean.pluginContent.flatMap { it.modules }
    return modules.filter { it.moduleName != null }
      .map {
        val name = it.moduleName!!
        val loadingRule = ModuleLoadingRule.create(it.loadingRule)
        if (it.value.isNullOrBlank()) {
          val configFile = "../${name.replace("/", ".")}.xml"
          Module.FileBasedModule(name, loadingRule, configFile)
        } else {
          val cDataContent = it.value!!
          Module.InlineModule(name, loadingRule, cDataContent)
        }
      }
      .toList()
  }

  fun supports(pluginBean: PluginBean): Boolean = pluginBean.pluginContent != null
}