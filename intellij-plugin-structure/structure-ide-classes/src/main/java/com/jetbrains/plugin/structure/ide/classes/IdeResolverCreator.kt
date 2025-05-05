/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.classes

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.InvalidIdeException
import java.nio.file.Path
import java.util.*

private const val COMPILED_IDE_RESOLVER_PROVIDER_CLASS_NAME = "com.jetbrains.plugin.structure.ide.jps.CompiledIdeResolverProvider"

object IdeResolverCreator {
  private val distributionIdeResolverProvider = DistributionIdeResolverProvider()

  @JvmStatic
  fun createIdeResolver(ide: Ide): Resolver = createIdeResolver(Resolver.ReadMode.FULL, ide)

  @JvmStatic
  fun createIdeResolver(readMode: Resolver.ReadMode, ide: Ide): Resolver = createIdeResolver(ide, IdeResolverConfiguration(readMode))

  @Throws(InvalidIdeException::class)
  @JvmStatic
  fun createIdeResolver(ide: Ide, configuration: IdeResolverConfiguration): Resolver {
    val idePath = ide.idePath
    val compiledIdeResolverProvider = getCompiledIdeResolverProvider(idePath)
    val resolverProvider = when {
      compiledIdeResolverProvider != null -> compiledIdeResolverProvider
      distributionIdeResolverProvider.supports(idePath) -> distributionIdeResolverProvider
      else -> throw InvalidIdeException(idePath, "Invalid IDE $ide at $idePath")
    }
    return resolverProvider.getIdeResolver(ide, configuration)
  }

  private fun getCompiledIdeResolverProvider(idePath: Path): IdeResolverProvider? {
    return tryLoadCompiledIdeResolverProvider()?.takeIf { it.supports(idePath) }
  }

  private fun tryLoadCompiledIdeResolverProvider(): IdeResolverProvider? {
    val ideManagerLoader = ServiceLoader.load(IdeResolverProvider::class.java)
    return ideManagerLoader.firstOrNull { it.javaClass.name == COMPILED_IDE_RESOLVER_PROVIDER_CLASS_NAME }
  }

}

