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