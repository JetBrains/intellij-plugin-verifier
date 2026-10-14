/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

class DependencyTest {
  @Test
  fun `content module declaration identifies only the declared module`() {
    val plugin = mockk<IdePlugin>()
    every { plugin.pluginId } returns "owner"
    val declaration = Dependency.ContentModuleDeclaration(plugin, "main")

    assertSame(plugin, declaration.plugin)
    assertEquals(NodeId("owner", "main"), declaration.nodeId)
    assertTrue(declaration.matches("main"))
    assertFalse(declaration.matches("extra"))
    assertFalse(declaration.matches("owner"))
    assertFalse(declaration.isTransitive)
    assertEquals("Content module 'main' declared by plugin 'owner'", declaration.toString())
  }

  @Test
  fun `content module declaration identity includes its owner and is distinct from a resolved module`() {
    val firstOwner = mockk<IdePlugin>()
    every { firstOwner.pluginId } returns "first"
    val secondOwner = mockk<IdePlugin>()
    every { secondOwner.pluginId } returns "second"
    val declaration = Dependency.ContentModuleDeclaration(firstOwner, "main")
    val sameDeclaration = Dependency.ContentModuleDeclaration(firstOwner, "main")
    val resolvedModule = Dependency.Module(firstOwner, "main")

    assertEquals(declaration, sameDeclaration)
    assertEquals(declaration.hashCode(), sameDeclaration.hashCode())
    assertNotEquals(declaration, Dependency.ContentModuleDeclaration(firstOwner, "extra"))
    assertNotEquals(declaration, Dependency.ContentModuleDeclaration(secondOwner, "main"))
    assertNotEquals(declaration, resolvedModule)
    assertEquals(declaration.nodeId, resolvedModule.nodeId)
  }

  @Test
  fun `content module declaration exposes its owner and module dependency for reporting`() {
    val plugin = mockk<IdePlugin>()
    every { plugin.pluginId } returns "owner"
    val declaration: Dependency = Dependency.ContentModuleDeclaration(plugin, "main")

    assertEquals("owner", declaration.id)
    assertEquals(PluginDependencyImpl("main", false, true), declaration.pluginDependency)
  }
}