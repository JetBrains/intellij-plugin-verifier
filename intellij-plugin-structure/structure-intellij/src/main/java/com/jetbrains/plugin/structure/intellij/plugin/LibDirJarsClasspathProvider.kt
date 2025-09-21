/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.listJars
import java.nio.file.Path

private const val MODULES_DIR = "modules"

/**
 * Return all JAR files available in the 'lib' directory of the plugin artifact path.
 */
class LibDirJarsClasspathProvider {
  fun getClasspath(path: Path): Classpath {
    val libDir = path.resolve(LIB_DIRECTORY)
    if (!libDir.exists()) {
      return Classpath.EMPTY
    }
    val jarPaths = libDir.listJars()
    return Classpath.of(jarPaths, ClasspathOrigin.PLUGIN_ARTIFACT)
  }
}