/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.classes.resolvers.Resolver.ReadMode.FULL
import com.jetbrains.plugin.structure.ide.CompiledIdeResolverTest.IdeResolverType.*
import com.jetbrains.plugin.structure.ide.classes.IdeResolverConfiguration
import com.jetbrains.plugin.structure.ide.classes.IdeResolverCreator
import com.jetbrains.plugin.structure.ide.classes.IdeResolverProvider
import com.jetbrains.plugin.structure.ide.jps.CompiledIdeResolverProvider
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.MockIde
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.nio.file.Path

@RunWith(Parameterized::class)
class CompiledIdeResolverTest(private val ideResolverType: IdeResolverType) {

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "ide-resolver={0}")
    fun ideResolverType(): List<Array<IdeResolverType>> = IdeResolverType.values().map { arrayOf(it) }
  }

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var ideResolverProvider: IdeResolverProvider

  @Before
  fun setUp() {
    ideResolverProvider = when (ideResolverType) {
      DEFAULT -> IdeResolverCreatorAdapter
      JPS -> CompiledIdeResolverProvider()
      SERVICE_LOADED_JPS -> {
        IdeResolverProviders.loadCompiledIdeResolverProvider()
          ?: throw IllegalStateException("IdeResolverProvider cannot be loaded via ServiceLoader")
      }
    }
  }

  @Test
  fun `compiled IDE resolvers are provided`() {
    val (idePath, _) = createCompiledIdeDirectories(temporaryFolder)

    val ideVersion = IdeVersion.createIdeVersion("IU-163.1")
    val ide = MockIde(ideVersion, idePath, bundledPlugins = emptyList())

    val ideResolver = ideResolverProvider.getIdeResolver(ide, IdeResolverConfiguration(readMode = FULL))
    with(ideResolver.allClassNames) {
      assertTrue(any { "com/example/LibPluginService".contentEquals(it) })
      assertTrue(any { "com/example/somePlugin/SomePluginService".contentEquals(it) })
    }
  }

  enum class IdeResolverType {
    DEFAULT,
    JPS,
    SERVICE_LOADED_JPS
  }

  object IdeResolverCreatorAdapter : IdeResolverProvider {
    override fun getIdeResolver(ide: Ide, configuration: IdeResolverConfiguration
    ) = IdeResolverCreator.createIdeResolver(ide, configuration)

    override fun supports(idePath: Path) = false
  }
}