/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests

import org.junit.Assert.assertEquals
import org.junit.Test

class ContentModuleFixtureTest {
  @Test
  fun `content DSL renders regular and embedded modules`() {
    val content = ContentModuleFixture.content {
      module("intellij.json.frontend.split")
      embeddedModule("intellij.json")
      module("intellij.json.backend")
    }

    assertEquals(
      """
      <content>
      <module name="intellij.json.frontend.split" />
      <module name="intellij.json" loading="embedded" />
      <module name="intellij.json.backend" />
      </content>
      """.trimIndent(),
      content
    )
  }
}
