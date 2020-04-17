/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.jdk

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import java.io.Closeable
import java.nio.file.Path

/**
 * Holder of class files of the JDK.
 */
data class JdkDescriptor(
  val jdkPath: Path,
  val jdkResolver: Resolver,
  val jdkVersion: JdkVersion
) : Closeable {
  override fun toString(): String = jdkPath.toAbsolutePath().toString()

  override fun close() = jdkResolver.close()
}