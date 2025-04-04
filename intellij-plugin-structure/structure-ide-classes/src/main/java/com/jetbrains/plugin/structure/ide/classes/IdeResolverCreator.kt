/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.classes

import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.listFiles
import com.jetbrains.plugin.structure.base.utils.listJars
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.DirectoryResolver
import com.jetbrains.plugin.structure.classes.resolvers.EMPTY_RESOLVER
import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.buildJarOrZipFileResolvers
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManagerImpl
import com.jetbrains.plugin.structure.ide.IdeManagerImpl.Companion.isCompiledCommunity
import com.jetbrains.plugin.structure.ide.IdeManagerImpl.Companion.isCompiledUltimate
import com.jetbrains.plugin.structure.ide.IdeManagerImpl.Companion.isDistributionIde
import com.jetbrains.plugin.structure.ide.InvalidIdeException
import com.jetbrains.plugin.structure.ide.classes.resolver.ProductInfoClassResolver
import com.jetbrains.plugin.structure.ide.getRepositoryLibrariesJars
import com.jetbrains.plugin.structure.ide.layout.InvalidIdeLayoutException
import java.nio.file.Path

object IdeResolverCreator {

  @JvmStatic
  fun createIdeResolver(ide: Ide): Resolver = createIdeResolver(Resolver.ReadMode.FULL, ide)

  @JvmStatic
  fun createIdeResolver(readMode: Resolver.ReadMode, ide: Ide): Resolver = createIdeResolver(ide, IdeResolverConfiguration(readMode))

  @JvmStatic
  fun createIdeResolver(ide: Ide, configuration: IdeResolverConfiguration): Resolver {
    val idePath = ide.idePath
    return when {
      isDistributionIde(idePath) -> getIdeResolverFromDistribution(ide, configuration)
      isCompiledCommunity(idePath) || isCompiledUltimate(idePath) -> getIdeResolverFromCompiledSources(idePath, configuration.readMode, ide)
      else -> throw InvalidIdeException(idePath, "Invalid IDE $ide at $idePath")
    }
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

  //TODO: Resolver created this way contains all libraries declared in the project,
  // including those that don't go to IDE distribution. So such a created resolver may
  // resolve classes differently than they are resolved when running IDE.
  // IDE sources can generate so-called "project-structure-mapping.json", which contains mapping
  // between compiled modules and jar files to which these modules are packaged in the final distribution.
  // We can use this mapping to construct a true resolver without irrelevant libraries.
  private fun getIdeResolverFromCompiledSources(idePath: Path, readMode: Resolver.ReadMode, ide: Ide): Resolver {
    val resolvers = arrayListOf<Resolver>()
    resolvers.closeOnException {
      resolvers += getJarsResolver(idePath.resolve("lib"), readMode, IdeFileOrigin.SourceLibDirectory(ide))
      resolvers += getRepositoryLibrariesResolver(idePath, readMode, ide)

      val compiledClassesRoot = IdeManagerImpl.getCompiledClassesRoot(idePath)!!
      compiledClassesRoot.listFiles().forEach { moduleRoot ->
        val fileOrigin = IdeFileOrigin.CompiledModule(ide, moduleRoot.simpleName)
        resolvers += DirectoryResolver(moduleRoot, fileOrigin, readMode)
      }

      if (isCompiledUltimate(idePath)) {
        resolvers += getJarsResolver(idePath.resolve("community").resolve("lib"), readMode, IdeFileOrigin.SourceLibDirectory(ide))
      }
      return CompositeResolver.create(resolvers)
    }
  }

  private fun getRepositoryLibrariesResolver(idePath: Path, readMode: Resolver.ReadMode, ide: Ide): Resolver {
    val jars = getRepositoryLibrariesJars(idePath)
    return CompositeResolver.create(buildJarOrZipFileResolvers(jars, readMode, IdeFileOrigin.RepositoryLibrary(ide)))
  }

}

