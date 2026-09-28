/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.DependsPluginDependency

@Suppress("TestFunctionName")
fun MandatoryV1Dependency(pluginId: String): DependsPluginDependency {
  return DependsPluginDependency(pluginId, false)
}