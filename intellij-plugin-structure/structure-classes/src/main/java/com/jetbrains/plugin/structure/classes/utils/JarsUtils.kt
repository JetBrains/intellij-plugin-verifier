package com.jetbrains.plugin.structure.classes.utils

import com.jetbrains.plugin.structure.base.utils.checkIfInterrupted
import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.classes.resolvers.JarFileResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.UnionResolver
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.AbstractFileFilter
import org.apache.commons.io.filefilter.FalseFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter
import java.io.File

object JarsUtils {

  fun collectJars(directory: File, filter: (File) -> Boolean, recursively: Boolean): List<File> =
      FileUtils.listFiles(directory, object : AbstractFileFilter() {
        override fun accept(file: File): Boolean {
          return file.name.endsWith(".jar", true) && filter(file)
        }
      }, if (recursively) TrueFileFilter.INSTANCE else FalseFileFilter.FALSE).toList()

  fun makeResolver(readMode: Resolver.ReadMode, jars: Iterable<File>): Resolver =
      UnionResolver.create(getResolversForJars(readMode, jars))

  fun makeResolver(jars: Iterable<File>): Resolver = makeResolver(Resolver.ReadMode.FULL, jars)

  private fun getResolversForJars(readMode: Resolver.ReadMode, jars: Iterable<File>): List<Resolver> {
    val resolvers = arrayListOf<Resolver>()
    try {
      jars.mapTo(resolvers) {
        checkIfInterrupted()
        JarFileResolver(it.toPath(), readMode)
      }
    } catch (e: Throwable) {
      resolvers.forEach { it.closeLogged() }
      throw e
    }
    return resolvers
  }

}
