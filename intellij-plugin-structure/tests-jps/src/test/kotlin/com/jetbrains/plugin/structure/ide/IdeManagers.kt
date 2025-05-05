/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import java.util.*

class IdeManagers {
  companion object {
    /**
     * Loads IDE manager for compiled binaries of an IDE via [ServiceLoader].
     */
    fun loadCompiledIdeManager(): IdeManager? {
      val ideManagerLoader = ServiceLoader.load(IdeManager::class.java)
      return ideManagerLoader.firstOrNull { it.javaClass.name == COMPILED_IDE_MANAGER_CLASS_NAME }
    }
  }
}

enum class IdeManagerType {
  DEFAULT,
  JPS,
  SERVICE_LOADED_JPS
}