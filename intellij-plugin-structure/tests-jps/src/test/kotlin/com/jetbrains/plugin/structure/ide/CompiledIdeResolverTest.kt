/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.classes.resolvers.Resolver.ReadMode.FULL
import com.jetbrains.plugin.structure.ide.classes.IdeResolverConfiguration
import com.jetbrains.plugin.structure.ide.jps.CompiledIdeResolverProvider
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.MockIde
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CompiledIdeResolverTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `compiled IDE resolvers are provided`() {
    val (idePath, _) = createCompiledIdeDirectories(temporaryFolder)

    val ideVersion = IdeVersion.createIdeVersion("IU-163.1")
    val ide = MockIde(ideVersion, idePath, bundledPlugins = emptyList())

    val ideResolverCreator = CompiledIdeResolverProvider()
    val ideResolver = ideResolverCreator.getIdeResolver(ide, IdeResolverConfiguration(readMode = FULL))
    with(ideResolver.allClassNames) {
      assertTrue(any { "com/example/LibPluginService".contentEquals(it) })
      assertTrue(any { "com/example/somePlugin/SomePluginService".contentEquals(it) })
    }
  }
}