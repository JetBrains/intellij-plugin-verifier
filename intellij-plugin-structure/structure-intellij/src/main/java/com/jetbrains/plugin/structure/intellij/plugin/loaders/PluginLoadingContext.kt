/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import java.nio.file.Path

internal sealed class PluginLoadingContext(
  open val artifactPath: Path,
  open val descriptorPath: String,
  open val validateDescriptor: Boolean,
  open val resourceResolver: ResourceResolver,
  open val parentPlugin: PluginCreator?,
  open val problemResolver: PluginCreationResultResolver,
  open val hasDotNetDirectory: Boolean = false
)

internal data class JarLoadingContext(
  val jarPath: Path,
  override val descriptorPath: String,
  override val validateDescriptor: Boolean,
  override val resourceResolver: ResourceResolver,
  override val parentPlugin: PluginCreator?,
  override val problemResolver: PluginCreationResultResolver,
  override val hasDotNetDirectory: Boolean = false
) : PluginLoadingContext(
  jarPath,
  descriptorPath,
  validateDescriptor,
  resourceResolver,
  parentPlugin,
  problemResolver,
  hasDotNetDirectory
)

