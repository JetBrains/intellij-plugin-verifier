/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.BinaryClassName
import com.jetbrains.plugin.structure.classes.utils.getBundleBaseName
import org.objectweb.asm.tree.ClassNode
import java.util.*

class FixedClassesResolver private constructor(
  private val classes: Map<String, ClassNode>,
  override val readMode: ReadMode,
  private val fileOrigin: FileOrigin,
  private val resourceBundles: Map<String, PropertyResourceBundle>
) : Resolver() {

  companion object {

    fun create(
      classes: Iterable<ClassNode>,
      fileOrigin: FileOrigin,
      readMode: ReadMode = ReadMode.FULL,
      propertyResourceBundles: Map<String, PropertyResourceBundle> = emptyMap()
    ): Resolver = FixedClassesResolver(
      classes.reversed().associateBy { it.name },
      readMode,
      fileOrigin,
      propertyResourceBundles
    )
  }

  private val packageSet = Packages()

  init {
    for (className in classes.keys) {
      packageSet.addClass(className)
    }
  }

  override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean) =
    classes.values
      .asSequence()
      .map { ResolutionResult.Found(it, fileOrigin) }
      .all(processor)

  @Deprecated("Use 'resolveClass(BinaryClassName)' instead")
  override fun resolveClass(className: String): ResolutionResult<ClassNode> {
    val classNode = classes[className] ?: return ResolutionResult.NotFound
    return ResolutionResult.Found(classNode, fileOrigin)
  }

  override fun resolveClass(className: BinaryClassName): ResolutionResult<ClassNode> {
    return resolveClass(className.toString())
  }

  override fun resolveExactPropertyResourceBundle(baseName: String, locale: Locale): ResolutionResult<PropertyResourceBundle> {
    val control = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_PROPERTIES)
    val bundleName = control.toBundleName(baseName, locale)
    val propertyResourceBundle = resourceBundles[bundleName] ?: return ResolutionResult.NotFound
    return ResolutionResult.Found(propertyResourceBundle, fileOrigin)
  }

  @Deprecated("Use 'allClassNames' property instead which is more efficient")
  override val allClasses
    get() = classes.keys

  override val allClassNames: Set<BinaryClassName>
    get() = allClasses

  override val allBundleNameSet: ResourceBundleNameSet
    get() = ResourceBundleNameSet(
      resourceBundles.keys
        .groupBy { getBundleBaseName(it) }
        .mapValues { it.value.toSet() }
    )

  @Deprecated("Use 'packages' property instead. This property may be slow on some file systems.")
  override val allPackages: Set<String>
    get() = packageSet.all

  override val packages: Set<String>
    get() = packageSet.entries

  override fun containsClass(className: String) = className in classes

  override fun containsPackage(packageName: String) = packageName in packageSet

  override fun close() = Unit

  override fun toString() = "Resolver of ${classes.size} predefined class" + (if (classes.size != 1) "es" else "")

}
