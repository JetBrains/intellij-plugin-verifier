package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.beans.ContentModuleDependencyBean
import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.beans.PluginDependenciesPluginBean
import com.jetbrains.plugin.structure.intellij.beans.PluginDependencyBean

internal const val INTELLIJ_MODULE_PREFIX = "com.intellij.modules."

internal val PluginBean.dependenciesV1: List<PluginDependencyBean>
  get() = dependencies
    ?.filter { it.dependencyId != null }
    ?: emptyList()

/**
 * Resolves `<dependencies>/<module>` elements representing Plugin Model v2
 * dependencies on plugin content modules.
 */
internal val PluginBean.contentModuleDependencies: List<ContentModuleDependencyBean>
  get() = dependenciesV2?.flatMap { it.modules }?.filter { it.moduleName != null } ?: emptyList()

/**
 * Resolves `<dependencies>/<plugin>` elements representing Plugin Model v2
 * dependencies on plugin main modules.
 */
internal val PluginBean.pluginMainModuleDependencies: List<PluginDependenciesPluginBean>
  get() = dependenciesV2?.flatMap { it.plugins }?.filter { it.dependencyId != null } ?: emptyList()

internal val PluginDependencyBean.isOptional: Boolean
   get() = optional ?: false

internal val PluginDependencyBean.isModule: Boolean
  get() = dependencyId?.startsWith(INTELLIJ_MODULE_PREFIX) == true

