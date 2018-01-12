package com.jetbrains.plugin.structure.ide

import com.google.common.base.Joiner
import com.google.common.io.Files
import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.utils.URLUtil
import com.jetbrains.plugin.structure.intellij.utils.xincludes.DefaultXIncludePathResolver
import com.jetbrains.plugin.structure.intellij.utils.xincludes.XIncludeException
import com.jetbrains.plugin.structure.intellij.utils.xincludes.XIncludePathResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.io.filefilter.WildcardFileFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.charset.Charset
import java.util.*

class IdeManagerImpl : IdeManager() {

  override fun createIde(ideDir: File): Ide = createIde(ideDir, null)

  override fun createIde(idePath: File, version: IdeVersion?): Ide {
    if (!idePath.exists()) {
      throw IllegalArgumentException("IDE file $idePath is not found")
    }
    val bundled = if (isSourceDir(idePath)) {
      getDummyPluginsFromSources(idePath)
    } else {
      readBundledPlugins(idePath)
    }
    val ideVersion = version ?: if (isSourceDir(idePath)) {
      readVersionFromSourcesDir(idePath)
    } else {
      readVersionFromBinaries(idePath)
    }
    return IdeImpl(idePath, ideVersion, bundled)
  }

  private fun isMacOs(): Boolean {
    val osName = System.getProperty("os.name")?.toLowerCase() ?: return false
    return osName.contains("mac os x") || osName.contains("darwin") || osName.contains("osx")
  }

  private fun readVersionFromBinaries(idePath: File): IdeVersion {
    if (isMacOs()) {
      val versionFile = File(idePath, "Resources/build.txt")
      if (versionFile.exists()) {
        return readBuildNumber(versionFile)
      }
    }
    val versionFile = File(idePath, "build.txt")
    if (!versionFile.exists()) {
      throw IllegalArgumentException(versionFile.toString() + " is not found")
    }
    return readBuildNumber(versionFile)
  }

  private fun readVersionFromSourcesDir(idePath: File): IdeVersion {
    val buildFile = File(idePath, "build.txt")
    if (buildFile.exists()) {
      return readBuildNumber(buildFile)
    }
    val communityBuildFile = File(idePath, "community/build.txt")
    if (communityBuildFile.exists()) {
      return readBuildNumber(communityBuildFile)
    }
    throw IllegalArgumentException("Unable to find IDE version file (build.txt or community/build.txt)")
  }

  private class PluginFromSourceXIncludePathResolver(private val descriptors: Map<String, File>) : DefaultXIncludePathResolver() {

    private fun resolveOutputDirectories(relativePath: String, base: String?): URL {
      val normalizedPath = if (relativePath.startsWith("./")) {
        "/META-INF/" + relativePath.substringAfter("./")
      } else {
        relativePath
      }

      val file = descriptors[normalizedPath]
      if (file != null) {
        try {
          return file.toURI().toURL()
        } catch (exc: Exception) {
          throw XIncludeException("File $file has an invalid URL presentation ", exc)
        }

      }
      throw XIncludeException("Unable to resolve " + normalizedPath + if (base != null) " against " + base else "")
    }

    override fun resolvePath(relativePath: String, base: String?): URL = try {
      val res = super.resolvePath(relativePath, base)
      //try the parent resolver
      URLUtil.openStream(res)
      res
    } catch (e: IOException) {
      resolveOutputDirectories(relativePath, base)
    }
  }

  private fun readBuildNumber(versionFile: File): IdeVersion {
    val buildNumberString = Files.toString(versionFile, Charset.defaultCharset()).trim { it <= ' ' }
    return IdeVersion.createIdeVersion(buildNumberString)
  }

  private fun getDummyPluginsFromSources(ideaDir: File): List<IdePlugin> = when {
    isUltimate(ideaDir) -> getDummyPlugins(getUltimateClassesRoot(ideaDir))
    isCommunity(ideaDir) -> getDummyPlugins(getCommunityClassesRoot(ideaDir))
    else -> throw IllegalArgumentException("Incorrect IDEA structure: $ideaDir. It must be Community or Ultimate sources root with compiled class files.")
  }

  private fun getDummyPlugins(root: File): List<IdePlugin> {
    val xmlFiles = FileUtils.listFiles(root, WildcardFileFilter("*.xml"), TrueFileFilter.TRUE)

    val pathResolver = getFromSourcesPathResolver(xmlFiles)
    return getDummyPlugins(xmlFiles, pathResolver)
  }

  private fun getDummyPlugins(xmlFiles: Collection<File>, pathResolver: XIncludePathResolver): List<IdePlugin> = xmlFiles
      .filter { "plugin.xml" == it.name }
      .map { it.absoluteFile.parentFile }
      .filter { "META-INF" == it.name && it.isDirectory && it.parentFile != null }
      .map { it.parentFile }
      .filter { it.isDirectory }
      .mapNotNull { createPlugin(it, pathResolver) }

  private fun createPlugin(pluginFile: File, pathResolver: XIncludePathResolver): IdePlugin? {
    try {
      val creationResult = IdePluginManager.createManager(pathResolver).createPlugin(pluginFile, false)
      return when (creationResult) {
        is PluginCreationSuccess -> creationResult.plugin
        is PluginCreationFail -> {
          val problems = creationResult.errorsAndWarnings
          LOG.info("Failed to read plugin " + pluginFile + ". Problems: " + Joiner.on(", ").join(problems))
          null
        }
      }
    } catch (e: Exception) {
      LOG.info("Unable to create plugin from sources: " + pluginFile, e)
    }

    return null
  }

  private fun getFromSourcesPathResolver(xmlFiles: Collection<File>): XIncludePathResolver {
    val xmlDescriptors = HashMap<String, File>()
    for (file in xmlFiles) {
      val path = file.absolutePath
      val parts = path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
      if (parts.size >= 2) {
        val key = "/" + parts[parts.size - 2] + "/" + parts[parts.size - 1]
        xmlDescriptors[key] = file
      }
    }
    return PluginFromSourceXIncludePathResolver(xmlDescriptors)
  }

  private fun readBundledPlugins(ideaDir: File): List<IdePlugin> {
    val plugins = readFromPluginsDir(ideaDir)
    val ideaCorePlugin = readIdeaCorePlugin(ideaDir)
    return if (ideaCorePlugin != null) {
      plugins + listOf(ideaCorePlugin)
    } else {
      plugins
    }
  }

  /**
   * The plugin 'IDEA CORE' is specially treated by com.intellij.ide.plugins.PluginManagerCore.
   * In the binary distribution the plugin resides in the /lib/resources.jar file.
   */
  private fun readIdeaCorePlugin(ideaDir: File): IdePlugin? {
    val resourcesJar = ideaDir.resolve("lib").resolve("resources.jar")
    if (!resourcesJar.exists()) {
      LOG.info("IDEA CORE plugin is not found the /lib/resources.jar")
      return null
    }
    return createPlugin(resourcesJar, DefaultXIncludePathResolver())
  }

  /**
   * Reads the plugins from the /plugins directory.
   */
  private fun readFromPluginsDir(ideaDir: File): List<IdePlugin> {
    val pluginsDir = File(ideaDir, "plugins")
    val pluginsFiles = pluginsDir.listFiles() ?: return emptyList()
    return pluginsFiles
        .filter { it.isDirectory }
        .mapNotNull { createPlugin(it, DefaultXIncludePathResolver()) }
  }

  companion object {

    private val LOG: Logger = LoggerFactory.getLogger(IdeManagerImpl::class.java)

    fun isSourceDir(dir: File): Boolean = File(dir, ".idea").isDirectory

    fun isUltimate(ideaDir: File): Boolean = File(ideaDir, "community/.idea").isDirectory && getUltimateClassesRoot(ideaDir).isDirectory

    fun isCommunity(ideaDir: File): Boolean = File(ideaDir, ".idea").isDirectory && getCommunityClassesRoot(ideaDir).isDirectory

    fun getUltimateClassesRoot(ideaDir: File): File = File(ideaDir, "out/classes/production")

    fun getCommunityClassesRoot(ideaDir: File): File = File(ideaDir, "out/production")
  }

}
