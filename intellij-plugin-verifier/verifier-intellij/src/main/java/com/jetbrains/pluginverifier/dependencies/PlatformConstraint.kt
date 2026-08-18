/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.enums.CpuArch
import com.jetbrains.plugin.structure.intellij.plugin.enums.OS

internal val PluginDependency.isPlatformConstraint: Boolean
  get() = OS.getByModule(id) != null || CpuArch.getByModule(id) != null
