/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.repositories

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo

fun interface CompatibilityPredicate {
  fun isCompatible(pluginInfo: PluginInfo, ideVersion: IdeVersion): Boolean

  companion object {
    @JvmField
    val ALWAYS_COMPATIBLE = CompatibilityPredicate { _, _ -> true }

    @JvmField
    val DEFAULT : CompatibilityPredicate = CompatibilityPredicate { pluginInfo, ideVersion ->
      pluginInfo.isCompatibleWith(ideVersion)
    }
  }
}