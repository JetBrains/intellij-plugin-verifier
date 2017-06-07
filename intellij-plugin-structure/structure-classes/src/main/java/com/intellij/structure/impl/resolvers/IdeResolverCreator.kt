package com.intellij.structure.impl.resolvers

import com.google.common.base.Predicate
import com.google.common.base.Predicates
import com.intellij.structure.ide.Ide
import com.intellij.structure.impl.domain.IdeManagerImpl
import com.intellij.structure.impl.utils.JarsUtils
import com.intellij.structure.resolvers.Resolver
import com.intellij.structure.resolvers.Resolver.createUnionResolver
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

/**
 * @author Sergey Patrikeev
 */
object IdeResolverCreator {

  private val HARD_CODED_LIB_FOLDERS = arrayOf("community/android/android/lib", "community/plugins/gradle/lib")

  private val LOG = LoggerFactory.getLogger(IdeResolverCreator::class.java)

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

    val jars = JarsUtils.collectJars(lib, Predicate<File> { file -> !file!!.name.endsWith("javac2.jar") }, false)

    return JarsUtils.makeResolver("Idea `lib` dir: " + lib.canonicalPath, jars)
  }

  @Throws(IOException::class)
  private fun getIdeaResolverFromSources(ideaDir: File): Resolver {
    val pools = arrayListOf<Resolver>()

    pools.add(getIdeResolverFromLibraries(ideaDir))

    if (IdeManagerImpl.isUltimate(ideaDir)) {
      pools.add(CompileOutputResolver(IdeManagerImpl.Companion.getUltimateClassesRoot(ideaDir)))
      pools.add(getIdeResolverFromLibraries(File(ideaDir, "community")))
      pools.addAll(hardCodedUltimateLibraries(ideaDir))
    } else if (IdeManagerImpl.isCommunity(ideaDir)) {
      pools.add(CompileOutputResolver(IdeManagerImpl.Companion.getCommunityClassesRoot(ideaDir)))
    } else {
      throw IllegalArgumentException("Incorrect IDEA sources: $ideaDir. It must be Community or Ultimate sources root with compiled class files")
    }

    return createUnionResolver("Idea dir: " + ideaDir.canonicalPath, pools)
  }

  private fun hardCodedUltimateLibraries(ideaDir: File): List<Resolver> = HARD_CODED_LIB_FOLDERS
      .map { File(ideaDir, it) }
      .filter { it.isDirectory }
      .mapNotNull { createResolverForLibDir(it) }

  private fun createResolverForLibDir(dir: File): Resolver? = try {
    JarsUtils.makeResolver(dir.name + " `lib` dir", JarsUtils.collectJars(dir, Predicates.alwaysTrue<File>(), false))
  } catch (e: Exception) {
    LOG.warn("Unable to read libraries from " + dir, e)
    null
  }

}
