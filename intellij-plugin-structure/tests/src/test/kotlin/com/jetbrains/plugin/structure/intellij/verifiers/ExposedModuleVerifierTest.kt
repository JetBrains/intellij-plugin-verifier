/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import com.jetbrains.plugin.structure.mocks.MockIdePlugin
import com.jetbrains.plugin.structure.mocks.SimpleProblemRegistrar
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ExposedModuleVerifierTest {
  private lateinit var verifier: ExposedModulesVerifier

  private lateinit var problemRegistrar: SimpleProblemRegistrar

  @Before
  fun setUp() {
    verifier = ExposedModulesVerifier()
    problemRegistrar = SimpleProblemRegistrar()
  }

  @Test
  fun `plugin exposes disallowed modules`() {
    val pluginId = "com.example.SomePlugin"
    val plugin = MockIdePlugin(
      pluginId = pluginId,
      pluginAliases = setOf("com.example.someModule", "com.intellij.someModule")
    )

    verifier.verify(plugin, problemRegistrar, PLUGIN_XML)

    with(problemRegistrar.problems) {
      assertEquals(1, size)
      val problem = first()
      assertEquals(
        "Invalid plugin descriptor 'plugin.xml'. Plugin declares a module with prohibited name: 'com.intellij.someModule' has prefix 'com.intellij'. Such modules cannot be declared by third party plugins.",
        problem.message
      )
    }
  }

  @After
  fun tearDown() {
    problemRegistrar.reset()
  }
}