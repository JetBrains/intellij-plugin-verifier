/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode.FAIL
import org.junit.Assert.assertTrue
import org.junit.Test

class IdeManagersTest {
  @Test
  fun `IDE manager is created with DSL`() {
    val ideManager = createIdeManager {
      missingLayoutFileMode = FAIL
    }
    assertTrue(ideManager is DispatchingIdeManager)
  }
}