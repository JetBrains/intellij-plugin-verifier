package com.jetbrains.plugin.structure.ide.classes

import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.classes.resolvers.*
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManagerImpl
import com.jetbrains.plugin.structure.ide.IdeManagerImpl.Companion.isCompiledCommunity
import com.jetbrains.plugin.structure.ide.IdeManagerImpl.Companion.isCompiledUltimate
import com.jetbrains.plugin.structure.ide.IdeManagerImpl.Companion.isDistributionIde
import com.jetbrains.plugin.structure.ide.InvalidIdeException
import com.jetbrains.plugin.structure.ide.getRepositoryLibrariesJars
import java.io.File

object IdeResolverCreator {

  @JvmStatic
  fun createIdeResolver(ide: Ide): Resolver = createIdeResolver(Resolver.ReadMode.FULL, ide)

  @JvmStatic
  fun createIdeResolver(readMode: Resolver.ReadMode, ide: Ide): Resolver {
    val idePath = ide.idePath
    return when {
      isDistributionIde(idePath) -> getJarsResolver(idePath.resolve("lib"), readMode, IdeFileOrigin.IdeLibDirectory)
      isCompiledCommunity(idePath) || isCompiledUltimate(idePath) -> getIdeResolverFromCompiledSources(idePath, readMode)
      else -> throw InvalidIdeException(idePath, "Invalid IDE $ide at $idePath")
    }
  }

  private fun getJarsResolver(
    directory: File,
    readMode: Resolver.ReadMode,
    parentOrigin: FileOrigin
  ): Resolver {
    if (!directory.isDirectory) {
      return EmptyResolver
    }

    val jars = directory.listFiles { file -> file.isJar() }.orEmpty().toList()
    return CompositeResolver.create(buildJarOrZipFileResolvers(jars, readMode, parentOrigin))
  }

  private fun getIdeResolverFromCompiledSources(idePath: File, readMode: Resolver.ReadMode): Resolver {
    val resolvers = arrayListOf<Resolver>()
    resolvers.closeOnException {
      resolvers += getJarsResolver(idePath.resolve("lib"), readMode, IdeFileOrigin.SourceLibDirectory)
      resolvers += getRepositoryLibrariesResolver(idePath, readMode)

      val compiledClassesRoot = IdeManagerImpl.getCompiledClassesRoot(idePath)!!
      for (moduleRoot in compiledClassesRoot.listFiles().orEmpty()) {
        val fileOrigin = IdeFileOrigin.CompiledModule(moduleRoot.name)
        resolvers += DirectoryResolver(moduleRoot.toPath(), fileOrigin, readMode)
      }

      if (isCompiledUltimate(idePath)) {
        resolvers += getJarsResolver(idePath.resolve("community").resolve("lib"), readMode, IdeFileOrigin.SourceLibDirectory)
      }
      return CompositeResolver.create(resolvers)
    }
  }

  private fun getRepositoryLibrariesResolver(idePath: File, readMode: Resolver.ReadMode): Resolver {
    val jars = getRepositoryLibrariesJars(idePath)
    return CompositeResolver.create(buildJarOrZipFileResolvers(jars, readMode, IdeFileOrigin.RepositoryLibrary))
  }

}

