/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.ide.jps.CompiledIdeManager
import com.jetbrains.plugin.structure.intellij.plugin.IdeTheme
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.PluginXmlBuilder
import com.jetbrains.plugin.structure.mocks.modify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CompiledIdeTest(private val ideManagerType: IdeManagerType) {
  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "ide-manager={0}")
    fun ideManagerType(): List<Array<IdeManagerType>> = listOf(arrayOf(IdeManagerType.DEFAULT), arrayOf(IdeManagerType.JPS))

  }

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var ideManager: IdeManager

  @Before
  fun setUp() {
    ideManager = when (ideManagerType) {
      IdeManagerType.DEFAULT -> IdeManager.createManager()
      IdeManagerType.JPS -> CompiledIdeManager()
      IdeManagerType.SERVICE_LOADED_JPS -> {
        IdeManagers.loadCompiledIdeManager()
          ?: throw IllegalStateException("Failed to load JPS IDE manager via ServiceLoader")
      }
    }
  }

  @Test
  fun `create idea from ultimate compiled sources`() {
    val (ideRoot, _) = createCompiledIdeDirectories(temporaryFolder)

    val ide = ideManager.createIde(ideRoot)
    assertEquals(IdeVersion.createIdeVersion("IU-163.1.2.3"), ide.version)
    assertEquals(1, ide.bundledPlugins.size)

    val plugin = ide.bundledPlugins[0]!!
    assertEquals(ideRoot.resolve("out").resolve("classes").resolve("production").resolve("somePlugin"), plugin.originalFile)
    assertEquals(listOf(IdeTheme("someTheme", true)), plugin.declaredThemes)
    assertEquals("someId", plugin.pluginId)
  }

  @Test
  fun `plugins bundled to idea may not have versions in descriptors`() {
    val ideaFolder = buildDirectory(temporaryFolder.newFolder("idea").toPath()) {
      file("build.txt", "IU-163.1.2.3")
      dir(".idea") { }
      dir("out") {
        dir("compilation") {
          dir("classes") {
            dir("production") {
              dir("Bundled") {
                dir("META-INF") {
                  file("plugin.xml") {
                    PluginXmlBuilder().modify {
                      name = "<name>Bundled</name>"
                      id = "<id>Bundled</id>"
                      vendor = "<vendor>JetBrains</vendor>"
                      description = "<description>Long enough test description for bundled plugin without version</description>"
                      changeNotes = "<change-notes>Short</change-notes>"
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    val ide = ideManager.createIde(ideaFolder)
    assertEquals(IdeVersion.createIdeVersion("IU-163.1.2.3"), ide.version)
    assertEquals(1, ide.bundledPlugins.size)
    val plugin = ide.bundledPlugins[0]!!
    assertEquals("Bundled", plugin.pluginId)
  }
}