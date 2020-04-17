/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.classes.resolvers

import java.nio.file.Path

interface FileOrigin {
  val parent: FileOrigin?
}

inline fun <reified T : FileOrigin> FileOrigin.findOriginOfType(): T? =
  generateSequence(this) { it.parent }.filterIsInstance<T>().firstOrNull()

inline fun <reified T : FileOrigin> FileOrigin.isOriginOfType(): Boolean = findOriginOfType<T>() != null

data class JarOrZipFileOrigin(val fileName: String, override val parent: FileOrigin) : FileOrigin

data class DirectoryFileOrigin(val directoryName: String, override val parent: FileOrigin): FileOrigin

data class JdkFileOrigin(val jdkPath: Path) : FileOrigin {
  override val parent: FileOrigin? = null
}