/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.plugin

import java.io.File
import java.nio.file.Path

interface PluginManager<out PluginType : Plugin> {
  @Deprecated(
    message = "Use method with java.nio.Path instead of java.io.File",
    replaceWith = ReplaceWith("createPlugin(pluginFile.toPath())")
  )
  fun createPlugin(pluginFile: File): PluginCreationResult<PluginType> =
    createPlugin(pluginFile.toPath())

  fun createPlugin(pluginFile: Path): PluginCreationResult<PluginType>
}