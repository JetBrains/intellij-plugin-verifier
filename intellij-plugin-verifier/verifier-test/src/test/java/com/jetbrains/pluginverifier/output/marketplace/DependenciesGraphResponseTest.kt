/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.output.marketplace

import com.jetbrains.pluginverifier.dependencies.*
import com.jetbrains.pluginverifier.response.DependenciesGraphDto
import com.jetbrains.pluginverifier.response.MissingDependenciesSetDto
import com.jetbrains.pluginverifier.response.convert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DependenciesGraphResponseTest {
  @Test
  fun `module ids include their providers throughout the exported graph`() {
    val root = ResolvedDependencyNode("root", "1.0")
    val first = ResolvedDependencyNode("shared.module", "2.0", moduleOwnerId = "first.provider")
    val second = ResolvedDependencyNode("shared.module", "2.0", moduleOwnerId = "second.provider")
    val dependency = ResolvedPluginDependency("shared.module", true, true, isContentModule = true)
    val missing = ResolvedMissingDependency(ResolvedPluginDependency("missing.plugin", false, false), "Not found")
    val graph = ResolvedDependenciesGraph(
      root,
      linkedSetOf(root, first, second),
      linkedSetOf(ResolvedDependencyEdge(root, first, dependency), ResolvedDependencyEdge(first, second, dependency)),
      linkedMapOf(first to setOf(missing), second to setOf(missing))
    )

    val response = graph.convert()

    val rootDto = DependenciesGraphDto.DependencyNodeDto("root", "1.0")
    val firstDto = DependenciesGraphDto.DependencyNodeDto("first.provider/shared.module", "2.0")
    val secondDto = DependenciesGraphDto.DependencyNodeDto("second.provider/shared.module", "2.0")
    val dependencyDto = DependenciesGraphDto.DependencyDto("shared.module", true, true)
    val missingDto = DependenciesGraphDto.MissingDependencyDto(
      DependenciesGraphDto.DependencyDto("missing.plugin", false, false), "Not found"
    )
    assertEquals(rootDto, response.start)
    assertEquals(listOf(rootDto, firstDto, secondDto), response.vertices)
    assertEquals(listOf(
      DependenciesGraphDto.DependencyEdgeDto(rootDto, firstDto, dependencyDto),
      DependenciesGraphDto.DependencyEdgeDto(firstDto, secondDto, dependencyDto)
    ), response.edges)
    assertEquals(listOf(
      MissingDependenciesSetDto(firstDto, setOf(missingDto)),
      MissingDependenciesSetDto(secondDto, setOf(missingDto))
    ), response.missingDependencies)
    assertTrue(response.edges.all { it.from in response.vertices && it.to in response.vertices })
  }

  @Test
  fun `local content module declarations include their owner ids`() {
    val root = ResolvedDependencyNode("owner", "1.0")
    val module = ResolvedDependencyNode("local.module", "1.0", moduleOwnerId = "owner", isContentModuleDeclaration = true)
    val graph = ResolvedDependenciesGraph(
      root,
      linkedSetOf(root, module),
      setOf(ResolvedDependencyEdge(root, module, ResolvedPluginDependency("local.module", false, true))),
      emptyMap()
    )

    val response = graph.convert()

    val moduleDto = DependenciesGraphDto.DependencyNodeDto("owner/local.module", "1.0")
    assertEquals(listOf(DependenciesGraphDto.DependencyNodeDto("owner", "1.0"), moduleDto), response.vertices)
    assertEquals(moduleDto, response.edges.single().to)
    assertEquals(DependenciesGraphDto.DependencyDto("local.module", false, true), response.edges.single().dependency)
  }

  @Test
  fun `ordinary plugin ids remain unchanged`() {
    val root = ResolvedDependencyNode("root", "1.0")
    val plugin = ResolvedDependencyNode("provider", "2.0", aliases = setOf("legacy.module"), isProductModule = true)
    val graph = ResolvedDependenciesGraph(
      root,
      linkedSetOf(root, plugin),
      setOf(ResolvedDependencyEdge(root, plugin, ResolvedPluginDependency("legacy.module", false, true))),
      emptyMap()
    )

    val response = graph.convert()

    assertEquals(DependenciesGraphDto(
      DependenciesGraphDto.DependencyNodeDto("root", "1.0"),
      listOf(DependenciesGraphDto.DependencyNodeDto("root", "1.0"), DependenciesGraphDto.DependencyNodeDto("provider", "2.0")),
      listOf(DependenciesGraphDto.DependencyEdgeDto(
        DependenciesGraphDto.DependencyNodeDto("root", "1.0"),
        DependenciesGraphDto.DependencyNodeDto("provider", "2.0"),
        DependenciesGraphDto.DependencyDto("legacy.module", false, true)
      )),
      emptyList()
    ), response)
  }
}