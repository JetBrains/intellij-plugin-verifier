/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.classes.resolvers.Resolver

/**
 * Denotes a plugin class's location type.
 * Examples of location types include JARs, classes directories or library directories.
 */
interface LocationKey {
  val name: String

  /**
   * Provide a location-specific classes locator configured with the specified [readMode].
   *
   * This is a factory method, implementations usually return a fresh instance of the corresponding [ClassesLocator].
   */
  fun getLocator(readMode: Resolver.ReadMode): ClassesLocator
}