/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.classes.resolvers

import org.jetbrains.org.objectweb.asm.tree.ClassNode
import java.util.*

object EmptyResolver : Resolver() {
  override val readMode
    get() = ReadMode.FULL

  override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean) = true

  override fun resolveClass(className: String) = ResolutionResult.NotFound

  override fun resolveExactPropertyResourceBundle(baseName: String, locale: Locale) = ResolutionResult.NotFound

  override fun containsClass(className: String) = false

  override fun containsPackage(packageName: String) = false

  override val allClasses get() = emptySet<String>()

  override val allPackages get() = emptySet<String>()

  override val allBundleNameSet: ResourceBundleNameSet get() = ResourceBundleNameSet(emptyMap())

  override fun toString() = "EmptyResolver"

  override fun close() = Unit

}
