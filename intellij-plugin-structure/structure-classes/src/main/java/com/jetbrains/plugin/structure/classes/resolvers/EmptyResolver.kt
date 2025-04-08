/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.classes.resolvers

import org.objectweb.asm.tree.ClassNode
import java.util.*

class EmptyResolver(override val name: String) : NamedResolver(name) {

  override val readMode = ReadMode.FULL

  override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean) = true

  override fun resolveClass(className: String) = ResolutionResult.NotFound

  override fun resolveExactPropertyResourceBundle(baseName: String, locale: Locale) = ResolutionResult.NotFound

  override fun containsClass(className: String) = false

  override fun containsPackage(packageName: String) = false

  override val allClasses = emptySet<String>()

  @Deprecated("Use 'packages' property instead. This property may be slow on some file systems.")
  override val allPackages = emptySet<String>()

  override val packages = emptySet<String>()

  override val allBundleNameSet = ResourceBundleNameSet(emptyMap())

  override fun toString() = name

  override fun close() = Unit
}
