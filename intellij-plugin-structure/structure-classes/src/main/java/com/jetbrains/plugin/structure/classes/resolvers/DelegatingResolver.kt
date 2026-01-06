/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.BinaryClassName
import org.objectweb.asm.tree.ClassNode
import java.util.*
import com.jetbrains.plugin.structure.classes.resolvers.Resolver.ReadMode

open class DelegatingNamedResolver(name: String, delegateProvider: () -> Resolver) : NamedResolver(name) {
  private val delegateResolver: Resolver by lazy { delegateProvider() }

  override val readMode: ReadMode
    get() = delegateResolver.readMode

  @Deprecated("Use 'allClassNames' property instead which is more efficient")
  override val allClasses: Set<String>
    get() = delegateResolver.allClasses
  override val allClassNames: Set<BinaryClassName>
    get() = delegateResolver.allClassNames

  @Deprecated("Use 'packages' property instead. This property may be slow on some file systems.")
  override val allPackages: Set<String>
    get() = delegateResolver.allPackages
  override val packages: Set<String>
    get() = delegateResolver.packages
  override val allBundleNameSet: ResourceBundleNameSet
    get() = delegateResolver.allBundleNameSet

  @Deprecated("Use 'resolveClass(BinaryClassName)' instead")
  override fun resolveClass(className: String) = delegateResolver.resolveClass(className)

  override fun resolveClass(className: BinaryClassName) = delegateResolver.resolveClass(className)

  override fun resolveExactPropertyResourceBundle(
    baseName: String, locale: Locale
  ): ResolutionResult<PropertyResourceBundle> = delegateResolver.resolveExactPropertyResourceBundle(baseName, locale)

  @Deprecated("Use 'containsClass(BinaryClassName)' instead")
  override fun containsClass(className: String): Boolean = delegateResolver.containsClass(className)

  override fun containsClass(className: BinaryClassName): Boolean = delegateResolver.containsClass(className)

  override fun containsPackage(packageName: String): Boolean = delegateResolver.containsPackage(packageName)

  override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean): Boolean =
    delegateResolver.processAllClasses(processor)

  override fun close(): Unit = delegateResolver.close()
}