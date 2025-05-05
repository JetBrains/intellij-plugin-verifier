/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.jps

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
import com.jetbrains.plugin.structure.ide.classes.IdeFileOrigin
import com.jetbrains.plugin.structure.ide.classes.IdeResolverConfiguration
import java.nio.file.Path

class CompiledIdeResolver {
  fun createIdeResolver(ide: Ide, configuration: IdeResolverConfiguration): Resolver {
    return getIdeResolver(ide, configuration.readMode)
  }

  fun supports(idePath: Path): Boolean {
    return isCompiledUltimate(idePath)
      || isCompiledCommunity(idePath)
  }

  //TODO: Resolver created this way contains all libraries declared in the project,
  // including those that don't go to IDE distribution. So such a created resolver may
  // resolve classes differently than they are resolved when running IDE.
  // IDE sources can generate so-called "project-structure-mapping.json", which contains mapping
  // between compiled modules and jar files to which these modules are packaged in the final distribution.
  // We can use this mapping to construct a true resolver without irrelevant libraries.
  private fun getIdeResolver(ide: Ide, readMode: Resolver.ReadMode): Resolver {
    val idePath = ide.idePath
    val resolvers = arrayListOf<Resolver>()
    resolvers.closeOnException {
      resolvers += getJarsResolver(idePath.resolve("lib"), readMode, IdeFileOrigin.SourceLibDirectory(ide))
      resolvers += getRepositoryLibrariesResolver(idePath, readMode, ide)

      val compiledClassesRoot = getCompiledClassesRoot(idePath)!!
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

  private fun getRepositoryLibrariesResolver(idePath: Path, readMode: Resolver.ReadMode, ide: Ide): Resolver {
    val jars = getRepositoryLibrariesJars(idePath)
    return CompositeResolver.create(buildJarOrZipFileResolvers(jars, readMode, IdeFileOrigin.RepositoryLibrary(ide)))
  }

}