/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.ide.classes.IdeResolverProvider
import java.util.*

private const val COMPILED_IDE_RESOLVER_PROVIDER_CLASS_NAME = "com.jetbrains.plugin.structure.ide.jps.CompiledIdeResolverProvider"

class IdeResolverProviders {
  companion object {
    /**
     * Loads IDE resolver provider for compiled binaries of an IDE via [ServiceLoader].
     */
    fun loadCompiledIdeResolverProvider(): IdeResolverProvider? {
      val ideManagerLoader = ServiceLoader.load(IdeResolverProvider::class.java)
      return ideManagerLoader.firstOrNull { it.javaClass.name == COMPILED_IDE_RESOLVER_PROVIDER_CLASS_NAME }
    }
  }
}