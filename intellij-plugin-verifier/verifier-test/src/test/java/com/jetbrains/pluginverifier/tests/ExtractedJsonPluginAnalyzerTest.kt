/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.analysis.ExtractedJsonPluginAnalyzer
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.modifiers.Modifiers.Modifier.PUBLIC
import com.jetbrains.pluginverifier.results.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.problems.UndeclaredPluginDependencyProblem
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.tests.mocks.MockIde
import com.jetbrains.pluginverifier.tests.mocks.MockIdePlugin
import com.jetbrains.pluginverifier.verifiers.resolution.toBinaryClassName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractedJsonPluginAnalyzerTest {
  private val jsonPluginAnalyzer = ExtractedJsonPluginAnalyzer()
  private val fileOrigin = object : FileOrigin {
    override val parent: FileOrigin? = null
  }

  private val targetIde = MockIde(IdeVersion.createIdeVersion("IC-243.16128"))

  private val plugin = MockIdePlugin()
  private val usage = ClassLocation("plugin.Usage", signature = null, Modifiers.of(PUBLIC), fileOrigin)

  @Test
  fun `class references an undeclared JSON package`() {
    val compatibilityProblems = mutableSetOf<CompatibilityProblem>(
      ClassNotFoundProblem(ClassReference("com.intellij.json.JsonElementType".toBinaryClassName()), usage)
    )
    val problems = jsonPluginAnalyzer.analyze(targetIde, plugin, compatibilityProblems)
    assertEquals(1, problems.size)
    val problem = problems.first()
    assertTrue(problem is UndeclaredPluginDependencyProblem)
    problem as UndeclaredPluginDependencyProblem
    assertEquals(
      "Plugin 'com.intellij.modules.json' is not declared in the plugin descriptor as a dependency for " +
        "class com.intellij.json.JsonElementType. " +
        "JSON support has been extracted to a separate plugin.",
      problem.fullDescription
    )
  }

  @Test
  fun `class references an another class that is not explicitly removed but the parent package is`() {
    val explicitlyRemovedClassInGeneralPackage = "com.intellij.json.JsonLexer"
    val compatibilityProblems = mutableSetOf<CompatibilityProblem>(
      ClassNotFoundProblem(ClassReference(explicitlyRemovedClassInGeneralPackage.toBinaryClassName()), usage)
    )
    val problems = jsonPluginAnalyzer.analyze(targetIde, plugin, compatibilityProblems)
    assertEquals(1, problems.size)
    val problem = problems.first()
    assertTrue(problem is UndeclaredPluginDependencyProblem)
    problem as UndeclaredPluginDependencyProblem
    assertEquals(
      "Plugin 'com.intellij.modules.json' is not declared in the plugin descriptor as a dependency for " +
        "class com.intellij.json.JsonLexer. " +
        "JSON support has been extracted to a separate plugin.",
      problem.fullDescription
    )
  }

}