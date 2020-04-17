/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin

sealed class PluginFileOrigin : FileOrigin {
  override val parent: FileOrigin? = null

  abstract val idePlugin: IdePlugin

  data class LibDirectory(override val idePlugin: IdePlugin) : PluginFileOrigin()
  data class ClassesDirectory(override val idePlugin: IdePlugin) : PluginFileOrigin()
  data class SingleJar(override val idePlugin: IdePlugin) : PluginFileOrigin()
  data class CompileServer(override val idePlugin: IdePlugin) : PluginFileOrigin()
}