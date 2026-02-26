/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests.dependencies

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraphBuilder
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.createIdeBundledOrPluginRepositoryDependencyFinder
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.plugin.PluginFileProvider
import com.jetbrains.pluginverifier.plugin.SizeLimitedPluginDetailsCache
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.repository.files.IdleFileLock
import com.jetbrains.pluginverifier.tests.mocks.MockIde
import com.jetbrains.pluginverifier.tests.mocks.MockIdePlugin
import com.jetbrains.pluginverifier.tests.mocks.MockPluginRepositoryAdapter
import com.jetbrains.pluginverifier.tests.mocks.createMockPluginInfo
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.Closeable

private val MOCK_IDE_MODULE_ID = "intellij.module.one"

class IdeDependencyFinderTest {
  @JvmField
  @Rule
  var tempFolder: TemporaryFolder = TemporaryFolder()

  private lateinit var ideModule: IdePlugin

  private lateinit var lock: IdleFileLock

  @Before
  fun setUp() {
    lock = IdleFileLock(tempFolder.newFile().toPath())
    ideModule = IdeModule(MOCK_IDE_MODULE_ID, hasPackagePrefix = false)
  }

  @Test
  fun `get all plugin transitive dependencies`() {
    /*
    Given following dependencies between plugins:

    `test` -> `someModule` (defined in `moduleContainer`)
    `test` -> `somePlugin`

    `myPlugin` -> `test`
    `myPlugin` -> `externalModule` (defined in external plugin `externalPlugin` which is impossible to download)
    `myPlugin` -> `com.intellij.modules.platform` (default module)

    Should find dependencies on `test`, `somePlugin`,
    `moduleContainer` and `com.intellij` (which is the 'IDEA CORE' plugin).

    Dependency on `com.intellij.modules.platform` must not be indicated.
    Dependency resolution on `externalPlugin` must fail.
     */
    val testPlugin = MockIdePlugin(
      pluginId = "test",
      pluginVersion = "1.0",
      dependencies = listOf(PluginDependencyImpl("someModule", false, true), PluginDependencyImpl("somePlugin", false, false))
    )
    val somePlugin = MockIdePlugin(
      pluginId = "somePlugin",
      pluginVersion = "1.0"
    )
    val moduleContainer = MockIdePlugin(
      pluginId = "moduleContainer",
      pluginVersion = "1.0",
      pluginAliases = setOf("someModule")
    )

    val ideVersion = IdeVersion.createIdeVersion("IU-144")
    val ide = MockIde(
      ideVersion,
      bundledPlugins = listOf(
        MockIdePlugin(
          pluginId = "com.intellij",
          pluginName = "IDEA CORE",
          originalFile = tempFolder.newFolder("idea.core").toPath(),
          pluginAliases = setOf(
            "com.intellij.modules.platform",
            "com.intellij.modules.lang",
            "com.intellij.modules.vcs",
            "com.intellij.modules.xml",
            "com.intellij.modules.xdebugger",
            "com.intellij.modules.java",
            "com.intellij.modules.ultimate",
            "com.intellij.modules.all"
          )
        ),
        testPlugin,
        somePlugin,
        moduleContainer,
        ideModule,
      )
    )

    val externalModuleDependency = PluginDependencyImpl("externalModule", false, true)
    val startPlugin = MockIdePlugin(
      pluginId = "myPlugin",
      pluginVersion = "1.0",
      dependencies = listOf(
        PluginDependencyImpl("test", true, false),
        externalModuleDependency,
        PluginDependencyImpl("com.intellij.modules.platform", false, true),
        PluginDependencyImpl(MOCK_IDE_MODULE_ID, false, true)
      )
    )

    val ideDependencyFinder = configureTestIdeDependencyFinder(ide)

    val (dependenciesGraph, _) = DependenciesGraphBuilder(ideDependencyFinder).buildDependenciesGraph(startPlugin, ide)

    val deps = dependenciesGraph.vertices.map { it.id }
    assertEquals(setOf("myPlugin", "test", "moduleContainer", "somePlugin", "com.intellij", MOCK_IDE_MODULE_ID), deps.toSet())

    assertEquals(setOf(MissingDependency(externalModuleDependency, "Failed to fetch plugin.")), dependenciesGraph.getDirectMissingDependencies())
  }

  private fun configureTestIdeDependencyFinder(ide: Ide): DependencyFinder {
    val pluginRepository = object : MockPluginRepositoryAdapter() {
      override fun getPluginsDeclaringModule(moduleId: String, ideVersion: IdeVersion?) =
        when (moduleId) {
          "externalModule" -> listOf(createMockPluginInfo("externalPlugin", "1.0"))
          MOCK_IDE_MODULE_ID -> listOf(createMockPluginInfo(MOCK_IDE_MODULE_ID, "1.0"))
          else -> emptyList()
        }

      override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String) =
        if (pluginId == "externalPlugin") createMockPluginInfo(pluginId, "1.0") else null
    }

    val pluginFileProvider = object : PluginFileProvider {
      override fun getPluginFile(pluginInfo: PluginInfo): PluginFileProvider.Result {
        if (pluginInfo.pluginId == "externalPlugin") {
          return PluginFileProvider.Result.Failed("Failed to fetch plugin.", Exception())
        }
        if (pluginInfo.pluginId == MOCK_IDE_MODULE_ID) {
          return PluginFileProvider.Result.Found(lock)
        }
        return PluginFileProvider.Result.NotFound("No need to be found")
      }
    }

    val pluginDetailsProvider = object : PluginDetailsProvider {
      override fun providePluginDetails(pluginInfo: PluginInfo, pluginFileLock: FileLock) = throw IllegalArgumentException()

      override fun providePluginDetails(pluginInfo: PluginInfo, idePlugin: IdePlugin) = PluginDetailsProvider.Result.Provided(
        PluginDetails(
          pluginInfo,
          idePlugin,
          emptyList(),
          IdePluginClassesLocations(
            idePlugin,
            Closeable { },
            emptyMap()
          ),
          null
        )
      )
    }

    val pluginDetailsCache = SizeLimitedPluginDetailsCache(10, pluginFileProvider, pluginDetailsProvider)
    return createIdeBundledOrPluginRepositoryDependencyFinder(ide, pluginRepository, pluginDetailsCache)
  }

}
