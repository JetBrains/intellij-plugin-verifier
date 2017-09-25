package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.base.utils.FileUtil
import com.jetbrains.plugin.structure.classes.locator.ClassesDirectoryLocator
import com.jetbrains.plugin.structure.classes.locator.CompileServerExtensionLocator
import com.jetbrains.plugin.structure.classes.locator.LibDirectoryLocator
import com.jetbrains.plugin.structure.intellij.extractor.ExtractedPluginFile
import com.jetbrains.plugin.structure.intellij.extractor.ExtractorFail
import com.jetbrains.plugin.structure.intellij.extractor.ExtractorSuccess
import com.jetbrains.plugin.structure.intellij.extractor.PluginExtractor
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.utils.StringUtil
import org.apache.commons.io.IOUtils
import org.objectweb.asm.tree.ClassNode
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.*

/**
 * @author Sergey Patrikeev
 */
class PluginResolver private constructor(private val plugin: IdePlugin,
                                         private val extractedPluginFile: ExtractedPluginFile) : Resolver() {

  private val resolver: Resolver
  private var isClosed: Boolean = false

  init {
    try {
      resolver = loadClasses(extractedPluginFile.actualPluginFile)
    } catch (e: Throwable) {
      IOUtils.closeQuietly(extractedPluginFile)
      throw e
    }
  }

  @Synchronized
  override fun close() {
    if (isClosed) {
      return
    }
    isClosed = true

    extractedPluginFile.use {
      resolver.close()
    }
  }

  override fun findClass(className: String): ClassNode? = resolver.findClass(className)

  override fun getClassLocation(className: String): Resolver? = resolver.getClassLocation(className)

  override fun getAllClasses(): Iterator<String> = resolver.allClasses

  override fun isEmpty(): Boolean = resolver.isEmpty

  override fun containsClass(className: String): Boolean = resolver.containsClass(className)

  override fun getClassPath(): List<File> = resolver.classPath

  override fun getFinalResolvers(): List<Resolver> = resolver.finalResolvers

  private fun loadClasses(file: File): Resolver {
    if (file.isDirectory) {
      return loadClassesFromDir(file)
    } else if (file.exists() && StringUtil.endsWithIgnoreCase(file.name, ".jar")) {
      return Resolver.createJarResolver(file)
    }
    throw IllegalArgumentException("Invalid plugin file extension: $file. It must be a directory or a jar file")
  }

  private fun loadClassesFromDir(pluginDirectory: File): Resolver {
    val resolvers = arrayListOf<Resolver>()
    try {
      for (classesLocator in CLASSES_LOCATORS) {
        resolvers.addAll(classesLocator.findClasses(plugin, pluginDirectory))
      }
    } catch (e: Throwable) {
      closeResolvers(resolvers)
      throw e
    }
    val distinctResolvers = resolvers.flatMap { it.finalResolvers }.distinctBy { it.classPath }
    return Resolver.createUnionResolver("Plugin resolver", distinctResolvers)
  }

  private fun closeResolvers(resolvers: List<Resolver>) {
    for (resolver in resolvers) {
      try {
        resolver.close()
      } catch (ce: Exception) {
        LOG.error("Unable to close resolver " + resolver, ce)
      }
    }
  }

  companion object {
    private val LOG = LoggerFactory.getLogger(PluginResolver::class.java)

    private val CLASSES_LOCATORS = Arrays.asList(ClassesDirectoryLocator(), LibDirectoryLocator(), CompileServerExtensionLocator())

    @Throws(IOException::class)
    fun createPluginResolver(plugin: IdePlugin, extractDirectory: File): Resolver {
      val pluginFile = plugin.originalFile
      if (pluginFile == null) {
        return Resolver.getEmptyResolver()
      } else if (!pluginFile.exists()) {
        throw IllegalArgumentException("Plugin file doesn't exist " + pluginFile)
      }
      if (pluginFile.isDirectory || FileUtil.isJarOrZip(pluginFile)) {
        if (FileUtil.isZip(pluginFile)) {
          val extractorResult = PluginExtractor.extractPlugin(pluginFile, extractDirectory)
          if (extractorResult is ExtractorSuccess) {
            val extractedPluginFile = extractorResult.extractedPlugin
            return PluginResolver(plugin, extractedPluginFile)
          } else {
            throw IOException((extractorResult as ExtractorFail).pluginProblem.message)
          }
        }
        return PluginResolver(plugin, ExtractedPluginFile(pluginFile, null))
      }
      throw IllegalArgumentException("Incorrect plugin file type $pluginFile: expected a directory, a .zip or a .jar archive")
    }
  }
}
