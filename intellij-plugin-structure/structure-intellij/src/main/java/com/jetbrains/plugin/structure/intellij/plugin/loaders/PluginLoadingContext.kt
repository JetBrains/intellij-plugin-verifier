/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import java.nio.file.Path

internal data class PluginLoadingContext(
  val artifactPath: Path,
  val descriptorPath: String,
  val validateDescriptor: Boolean,
  val resourceResolver: ResourceResolver,
  val parentPlugin: PluginCreator?,
  val problemResolver: PluginCreationResultResolver,
  val hasDotNetDirectory: Boolean = false
)