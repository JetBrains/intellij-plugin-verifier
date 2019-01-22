package com.jetbrains.plugin.structure.ide.classes

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.SystemProperties
import com.jetbrains.plugin.structure.classes.resolvers.ClassFilesResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import com.jetbrains.plugin.structure.classes.utils.JarsUtils
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManagerImpl
import com.jetbrains.plugin.structure.ide.IdeManagerImpl.Companion.isSourceDir
import com.jetbrains.plugin.structure.ide.util.loadProject
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.library.JpsOrderRootType
import java.io.File
import java.io.IOException

object IdeResolverCreator {

  @JvmStatic
  fun createIdeResolver(ide: Ide): Resolver = createIdeResolver(Resolver.ReadMode.FULL, ide)

  @JvmStatic
  fun createIdeResolver(readMode: Resolver.ReadMode, ide: Ide): Resolver {
    val idePath = ide.idePath
    return if (isSourceDir(idePath)) {
      getIdeaResolverFromSources(idePath, readMode)
    } else {
      getIdeResolverFromLibraries(idePath, readMode)
    }
  }

  @Throws(IOException::class)
  private fun getIdeResolverFromLibraries(ideDir: File, readMode: Resolver.ReadMode): Resolver {
    val lib = File(ideDir, "lib")
    if (!lib.isDirectory) {
      throw IOException("Directory \"lib\" is not found (should be found at $lib)")
    }

    val jars = JarsUtils.collectJars(lib, { true }, false)

    return JarsUtils.makeResolver(readMode, jars)
  }

  @Throws(IOException::class)
  private fun getIdeaResolverFromSources(ideaDir: File, readMode: Resolver.ReadMode): Resolver {
    val pools = arrayListOf<Resolver>()

    pools.add(getIdeResolverFromLibraries(ideaDir, readMode))

    when {
      IdeManagerImpl.isUltimate(ideaDir) -> {
        pools += ClassFilesResolver(IdeManagerImpl.getUltimateClassesRoot(ideaDir)!!, readMode)
        pools += getIdeResolverFromLibraries(File(ideaDir, "community"), readMode)
        pools += getLibrariesResolver(ideaDir, readMode)
      }
      IdeManagerImpl.isCommunity(ideaDir) -> pools += ClassFilesResolver(IdeManagerImpl.getCommunityClassesRoot(ideaDir)!!, readMode)
      else -> throw IllegalArgumentException("Incorrect IDEA sources: $ideaDir. It must be Community or Ultimate sources root with compiled class files")
    }

    return UnionResolver.create(pools)
  }

  private fun getLibrariesResolver(ideaDir: File, readMode: Resolver.ReadMode): Resolver {
    val jarLibraries = getJarLibraries(ideaDir.absolutePath)
    return JarsUtils.makeResolver(readMode, jarLibraries)
  }

  private fun getJarLibraries(projectPath: String): List<File> {
    val pathVariables = createPathVariables()
    val project = loadProject(projectPath, pathVariables)
    return JpsJavaExtensionService.dependencies(project)
        .productionOnly()
        .runtimeOnly()
        .libraries
        .flatMap { it.getFiles(JpsOrderRootType.COMPILED) }
        .distinctBy { it.path }
        .filter { it.isFile && it.name.endsWith(".jar") }
  }

  private fun createPathVariables(): Map<String, String> {
    val m2Repo = FileUtil.toSystemIndependentName(File(SystemProperties.getUserHome(), ".m2/repository").absolutePath)
    return mapOf("MAVEN_REPOSITORY" to m2Repo)
  }

}

