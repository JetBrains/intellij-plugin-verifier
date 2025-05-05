/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import org.junit.Assert.assertNotNull
import org.junit.Test

internal const val COMPILED_IDE_MANAGER_CLASS_NAME = "com.jetbrains.plugin.structure.ide.jps.CompiledIdeManager"

class ServiceLoaderTest {
  @Test
  fun `JPS-based compiled IDE Manager is resolved via ServiceLoader`() {
    val compiledIdeManager = IdeManagers.loadCompiledIdeManager()
    assertNotNull(compiledIdeManager)
  }
}