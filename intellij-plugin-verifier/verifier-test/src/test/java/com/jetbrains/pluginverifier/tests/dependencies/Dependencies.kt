/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.DependsPluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.INTELLIJ_LEGACY_MODULES_PREFIX

@Suppress("TestFunctionName")
fun MandatoryV1Dependency(pluginId: String): DependsPluginDependency {
  return DependsPluginDependency(pluginId, false)
}


@Suppress("TestFunctionName")
fun OptionalV1Dependency(pluginId: String): DependsPluginDependency {
  return DependsPluginDependency(pluginId, true)
}

@Suppress("TestFunctionName")
fun MandatoryLegacyModuleV1Dependency(pluginId: String): DependsPluginDependency {
  require(pluginId.startsWith(INTELLIJ_LEGACY_MODULES_PREFIX)) {
    "Legacy module dependency should start with '$INTELLIJ_LEGACY_MODULES_PREFIX'"
  }
  return DependsPluginDependency(pluginId, false)
}


