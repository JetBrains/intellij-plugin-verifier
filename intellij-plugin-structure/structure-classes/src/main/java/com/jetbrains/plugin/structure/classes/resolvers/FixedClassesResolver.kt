/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.classes.utils.getBundleBaseName
import org.jetbrains.org.objectweb.asm.tree.ClassNode
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

  private val packageSet = PackageSet()

  init {
    for (className in classes.keys) {
      packageSet.addPackagesOfClass(className)
    }
  }

  override fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean) =
    classes.values
      .asSequence()
      .map { ResolutionResult.Found(it, fileOrigin) }
      .all(processor)

  override fun resolveClass(className: String): ResolutionResult<ClassNode> {
    val classNode = classes[className] ?: return ResolutionResult.NotFound
    return ResolutionResult.Found(classNode, fileOrigin)
  }

  override fun resolveExactPropertyResourceBundle(baseName: String, locale: Locale): ResolutionResult<PropertyResourceBundle> {
    val control = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_PROPERTIES)
    val bundleName = control.toBundleName(baseName, locale)
    val propertyResourceBundle = resourceBundles[bundleName] ?: return ResolutionResult.NotFound
    return ResolutionResult.Found(propertyResourceBundle, fileOrigin)
  }

  override val allClasses
    get() = classes.keys

  override val allBundleNameSet: ResourceBundleNameSet
    get() = ResourceBundleNameSet(
      resourceBundles.keys
        .groupBy { getBundleBaseName(it) }
        .mapValues { it.value.toSet() }
    )

  override val allPackages: Set<String>
    get() = packageSet.getAllPackages()

  override fun containsClass(className: String) = className in classes

  override fun containsPackage(packageName: String) = packageSet.containsPackage(packageName)

  override fun close() = Unit

  override fun toString() = "Resolver of ${classes.size} predefined class" + (if (classes.size != 1) "es" else "")

}
