/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.jar

import java.io.IOException
import java.nio.file.Path

class JarFileSystemProviderException(
  message: String,
  val path: Path,
  val provider: JarFileSystemProvider,
  cause: Throwable
) : IOException(message, cause)