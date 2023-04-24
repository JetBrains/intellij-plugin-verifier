/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.nio.file.Path

/**
 * Locates plugin classes in a specific location.
 */
interface ClassesLocator {
  /**
   * A class location type.
   */
  val locationKey: LocationKey

  /**
   * Locates plugin classes and returns one or more [Resolver]s with class locations.
   */
  fun findClasses(idePlugin: IdePlugin, pluginFile: Path): List<Resolver>
}