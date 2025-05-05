/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.classes

import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.listJars
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.EMPTY_RESOLVER
import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.buildJarOrZipFileResolvers
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManagerImpl.Companion.isDistributionIde
import com.jetbrains.plugin.structure.ide.InvalidIdeException
import com.jetbrains.plugin.structure.ide.classes.resolver.ProductInfoClassResolver
import com.jetbrains.plugin.structure.ide.layout.InvalidIdeLayoutException
import java.nio.file.Path

class DistributionIdeResolverCreator {
  @Throws(InvalidIdeException::class)
  fun createIdeResolver(ide: Ide, configuration: IdeResolverConfiguration): Resolver {
    val idePath = ide.idePath
    if (isDistributionIde(idePath)) {
      return getIdeResolverFromDistribution(ide, configuration)
    } else {
      throw InvalidIdeException(idePath, "Invalid IDE $ide at $idePath")
    }
  }

  fun supports(idePath: Path): Boolean = with(idePath) {
    return resolve("lib").isDirectory &&
      !resolve(".idea").isDirectory
  }

  private fun getJarsResolver(
    libDirectory: Path,
    readMode: Resolver.ReadMode,
    parentOrigin: FileOrigin
  ): Resolver {
    if (!libDirectory.isDirectory) {
      return EMPTY_RESOLVER
    }

    val jars = libDirectory.listJars()
    val antJars = libDirectory.resolve("ant").resolve("lib").listJars()
    val moduleJars = libDirectory.resolve("modules").listJars()
    return CompositeResolver.create(buildJarOrZipFileResolvers(jars + antJars + moduleJars, readMode, parentOrigin))
  }

  private fun getIdeResolverFromDistribution(ide: Ide, resolverConfiguration: IdeResolverConfiguration): Resolver = with(ide) {
    return if (ProductInfoClassResolver.supports(idePath)) {
      getProductInfoClassResolver(ide, resolverConfiguration)
    } else {
      getJarsResolver(idePath.resolve("lib"), resolverConfiguration.readMode, IdeFileOrigin.IdeLibDirectory(ide))
    }
  }

  private fun getProductInfoClassResolver(ide: Ide, resolverConfiguration: IdeResolverConfiguration): ProductInfoClassResolver {
    try {
      return ProductInfoClassResolver.of(ide, resolverConfiguration)
    } catch (e: InvalidIdeLayoutException) {
      throw InvalidIdeException(ide.idePath, e)
    }
  }
}