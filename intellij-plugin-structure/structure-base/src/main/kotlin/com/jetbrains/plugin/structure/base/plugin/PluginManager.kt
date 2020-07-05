/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.plugin

import java.io.File
import java.io.InputStream

interface PluginManager<out PluginType : Plugin> {
  fun createPlugin(pluginFile: File): PluginCreationResult<PluginType>

  fun createPlugin(pluginFileContent: InputStream, pluginFileName: String): PluginCreationResult<PluginType>
}