/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import com.jetbrains.plugin.structure.jar.PluginDescriptorResult
import org.jdom2.Document

internal fun PluginDescriptorResult.Found.loadXml(): Document = inputStream.use {
  JDOMUtil.loadDocument(it)
}