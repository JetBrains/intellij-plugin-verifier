/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.BinaryClassName
import org.objectweb.asm.tree.ClassNode
import java.io.Closeable
import java.io.IOException
import java.util.*

/**
 * Resolves class files by name.
 */
abstract class Resolver : Closeable {

  /**
   * Read mode used to specify whether this resolver reads [ClassNode]s fully,
   * including methods' code, debug frames, or only classes' signatures.
   */
  enum class ReadMode {
    FULL, SIGNATURES
  }

  /**
   * Read mode this resolved is opened with.
   */
  abstract val readMode: ReadMode

  /**
   * Returns the *binary* names of all the contained classes.
   */
  @Deprecated(message = "Use 'allClassNames' property instead which is more efficient")
  abstract val allClasses: Set<String>

  /**
   * Returns the *binary* names of all the contained classes.
   */
  abstract val allClassNames: Set<BinaryClassName>

  /**
   * Returns binary names of all contained packages and their super-packages.
   *
   * For example, if this Resolver contains classes of a package `com/example/utils`
   * then [allPackages] contains `com`, `com/example` and `com/example/utils`.
   */
  @Deprecated(message = "Use 'packages' property instead. This property may be slow on some file systems.")
  abstract val allPackages: Set<String>

  /**
   * Returns binary names of all contained packages. Their superpackages are *not* available in this collection.
   *
   * For example, if this Resolver contains classes of a package `com/example/utils` and `org.example.impl`.
   * then [packages] contains these two elements.
   */
  abstract val packages: Set<String>

  /**
   * Returns data structure used to obtain bundle names contained in this resolver.
   */
  abstract val allBundleNameSet: ResourceBundleNameSet

  /**
   * Resolves class with specified binary name.
   */
  @Deprecated("Use 'resolveClass(BinaryClassName)' instead")
  abstract fun resolveClass(className: String): ResolutionResult<ClassNode>

  /**
   * Resolves class with specified binary name.
   */
  abstract fun resolveClass(className: BinaryClassName): ResolutionResult<ClassNode>

  /**
   * Resolves property resource bundle with specified **exact** base name and locale.
   * If no property bundle is available for that locale, the search in candidate locales **is not performed**.
   */
  abstract fun resolveExactPropertyResourceBundle(baseName: String, locale: Locale): ResolutionResult<PropertyResourceBundle>

  /**
   * Returns true if `this` Resolver contains the given class. It may be faster
   * than checking [.findClass] is not null.
   */
  abstract fun containsClass(className: String): Boolean

  /**
   * Returns true if `this` Resolver contains the given package,
   * specified with binary name ('/'-separated). It may be faster
   * than fetching [allPackages] and checking for presence in it.
   */
  abstract fun containsPackage(packageName: String): Boolean

  /**
   * Runs the given [processor] on every class contained in _this_ [Resolver].
   * The [processor] returns `true` to continue processing and `false` to stop.
   */
  @Throws(IOException::class)
  abstract fun processAllClasses(processor: (ResolutionResult<ClassNode>) -> Boolean): Boolean

}