/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.filtering

import com.jetbrains.plugin.structure.base.BinaryClassName
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations

/**
 * Allows to specify which classes constitute the plugin's class loader and which classes should be verified.
 */
interface ClassesSelector {

  fun getClassLoader(classesLocations: IdePluginClassesLocations): List<Resolver>

  fun getClassesForCheck(classesLocations: IdePluginClassesLocations): Set<BinaryClassName>
}