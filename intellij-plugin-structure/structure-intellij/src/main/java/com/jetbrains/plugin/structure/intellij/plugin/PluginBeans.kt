package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.beans.PluginDependenciesPluginBean
import com.jetbrains.plugin.structure.intellij.beans.PluginDependencyBean
import com.jetbrains.plugin.structure.intellij.beans.PluginModuleBean

internal const val INTELLIJ_MODULE_PREFIX = "com.intellij.modules."

internal val PluginBean.dependenciesV1: List<PluginDependencyBean>
  get() = dependencies
    ?.filter { it.dependencyId != null }
    ?: emptyList()

internal val PluginBean.dependentModules: List<PluginModuleBean>
  get() = dependenciesV2?.modules?.filter { it.moduleName != null } ?: emptyList()

internal val PluginBean.dependentPlugins: List<PluginDependenciesPluginBean>
  get() = dependenciesV2?.plugins?.filter { it.dependencyId != null } ?: emptyList()

internal val PluginDependenciesPluginBean.isModule: Boolean
  get() = dependencyId.startsWith(INTELLIJ_MODULE_PREFIX)

internal val PluginDependencyBean.isOptional: Boolean
   get() = optional ?: false

