package com.jetbrains.plugin.structure.ide.classes

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.SystemProperties
import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.classes.resolvers.*
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManagerImpl
import com.jetbrains.plugin.structure.ide.IdeManagerImpl.Companion.isCompiledCommunity
import com.jetbrains.plugin.structure.ide.IdeManagerImpl.Companion.isCompiledUltimate
import com.jetbrains.plugin.structure.ide.IdeManagerImpl.Companion.isDistributionIde
import com.jetbrains.plugin.structure.ide.InvalidIdeException
import com.jetbrains.plugin.structure.ide.util.loadProject
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.library.JpsOrderRootType
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
    return CompositeResolver.create(buildJarFileResolvers(jars, readMode, parentOrigin))
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
    val jars = getRepositoryLibraries(idePath)
    return CompositeResolver.create(buildJarFileResolvers(jars, readMode, IdeFileOrigin.RepositoryLibrary))
  }

  private fun getRepositoryLibraries(projectPath: File): List<File> {
    val pathVariables = createPathVariables()
    val project = loadProject(projectPath.absoluteFile, pathVariables)
    return JpsJavaExtensionService.dependencies(project)
      .productionOnly()
      .runtimeOnly()
      .libraries
      .flatMap { it.getFiles(JpsOrderRootType.COMPILED) }
      .distinctBy { it.path }
      .filter { it.isJar() }
  }

  private fun createPathVariables(): Map<String, String> {
    val m2Repo = FileUtil.toSystemIndependentName(File(SystemProperties.getUserHome(), ".m2/repository").absolutePath)
    return mapOf("MAVEN_REPOSITORY" to m2Repo)
  }

}

