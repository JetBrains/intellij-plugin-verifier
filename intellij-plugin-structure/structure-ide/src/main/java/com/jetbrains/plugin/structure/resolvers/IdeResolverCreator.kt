package com.jetbrains.plugin.structure.resolvers

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.SystemProperties
import com.jetbrains.plugin.structure.classes.resolvers.CompileOutputResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver.createUnionResolver
import com.jetbrains.plugin.structure.classes.utils.JarsUtils
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManagerImpl
import com.jetbrains.plugin.structure.ide.loadProject
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.library.JpsOrderRootType
import java.io.File
import java.io.IOException

/**
 * @author Sergey Patrikeev
 */
object IdeResolverCreator {

  /**
   * Creates a resolver for the given Ide.
   * <p>If {@code ide} represents a binary IDE distribution the result consists of the .jar files under
   * the <i>{ide.home}/lib</i> directory (not including the subdirectories of <i>lib</i> itself).</p>
   * <p>If {@code ide} represents an IDE compile output the result consists of the class files under the build-directory
   * (for Ultimate it is <i>{ide.home}/out/classes/production</i>)</p>
   *
   * @param ide ide for which to create a resolver
   * @throws IOException if error occurs during attempt to read a class file or an Ide has an incorrect directories structure
   * @return resolver of classes for the given Ide
   */
  @Throws(IOException::class)
  @JvmStatic
  fun createIdeResolver(ide: Ide): Resolver {
    val idePath = ide.idePath
    if (IdeManagerImpl.isSourceDir(idePath)) {
      return getIdeaResolverFromSources(idePath)
    } else {
      return getIdeResolverFromLibraries(idePath)
    }
  }

  @Throws(IOException::class)
  private fun getIdeResolverFromLibraries(ideDir: File): Resolver {
    val lib = File(ideDir, "lib")
    if (!lib.isDirectory) {
      throw IOException("Directory \"lib\" is not found (should be found at $lib)")
    }

    val jars = JarsUtils.collectJars(lib, { file -> !file!!.name.endsWith("javac2.jar") }, false)

    return JarsUtils.makeResolver("Idea `lib` dir: " + lib.canonicalPath, jars)
  }

  @Throws(IOException::class)
  private fun getIdeaResolverFromSources(ideaDir: File): Resolver {
    val pools = arrayListOf<Resolver>()

    pools.add(getIdeResolverFromLibraries(ideaDir))

    if (IdeManagerImpl.isUltimate(ideaDir)) {
      pools.add(CompileOutputResolver(IdeManagerImpl.getUltimateClassesRoot(ideaDir)))
      pools.add(getIdeResolverFromLibraries(File(ideaDir, "community")))
      pools.add(getLibrariesResolver(ideaDir))
    } else if (IdeManagerImpl.isCommunity(ideaDir)) {
      pools.add(CompileOutputResolver(IdeManagerImpl.getCommunityClassesRoot(ideaDir)))
    } else {
      throw IllegalArgumentException("Incorrect IDEA sources: $ideaDir. It must be Community or Ultimate sources root with compiled class files")
    }

    return createUnionResolver("Idea dir: " + ideaDir.canonicalPath, pools)
  }

  private fun getLibrariesResolver(ideaDir: File): Resolver {
    val jarLibraries = getJarLibraries(ideaDir.absolutePath)
    return JarsUtils.makeResolver("IDE $ideaDir libraries", jarLibraries)
  }

  private fun getJarLibraries(projectPath: String): List<File> {
    val pathVariables = createPathVariables()
    val project = loadProject(projectPath, pathVariables)
    return JpsJavaExtensionService.dependencies(project)
        .productionOnly()
        .runtimeOnly()
        .libraries
        .flatMap { it.getFiles(JpsOrderRootType.COMPILED) }
        .filter { it.isFile && it.name.endsWith(".jar") }
  }

  private fun createPathVariables(): Map<String, String> {
    val m2Repo = FileUtil.toSystemIndependentName(File(SystemProperties.getUserHome(), ".m2/repository").absolutePath)
    return mapOf("MAVEN_REPOSITORY" to m2Repo)
  }

}

