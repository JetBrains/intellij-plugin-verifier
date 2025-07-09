/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.classes.plugin

import com.jetbrains.plugin.structure.base.plugin.Settings
import com.jetbrains.plugin.structure.base.utils.createDir
import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager

data class ClassSearchContext(
  val archiveManager: PluginArchiveManager
) {
  companion object {
    @JvmStatic
    val DEFAULT = ClassSearchContext(
      archiveManager = PluginArchiveManager(extractDirectory = Settings.EXTRACT_DIRECTORY.getAsPath().createDir())
    )
  }
}