package com.jetbrains.plugin.structure.ide

import com.google.common.base.Joiner
import com.google.common.io.Files
import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.PluginXmlXIncludePathResolver
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

  /**
   * [XIncludePathResolver] that resolves the plugin descriptors (`plugin.xml`)
   * from the locally built IDE sources.
   *
   * The [xmlFiles] is a mapping from shortened .xml paths to XML files
   * under the `/out/` directory of the locally built IDE.
   * Those files may be referenced as <x-include> elements from `plugin.xml`.
   *
   * For example, the [xmlFiles] may be
   * ```
   * /META-INF/one.xml -> <path to one.xml >
   * /META-INF/two.xml -> <path to two.xml>
   * ```
   *
   * and a declaration in a `plugin.xml`:
   * ```
   * <xi:include href="/META-INF/one.xml" xpointer="xpointer(/idea-plugin/\*)"/>
   * ```
   */
  private class PluginFromSourceXIncludePathResolver(private val xmlFiles: Map<String, File>) : DefaultXIncludePathResolver() {

    private fun resolveOutputDirectories(relativePath: String, base: String?): URL {
      val normalizedPath = if (relativePath.startsWith("./")) {
        "/META-INF/" + relativePath.substringAfter("./")
      } else {
        relativePath
      }

      val xmlFile = xmlFiles[normalizedPath]
      if (xmlFile != null) {
        try {
          return xmlFile.toURI().toURL()
        } catch (exc: Exception) {
          throw XIncludeException(exc)
        }
      }
      throw XIncludeException("Unable to resolve " + normalizedPath + if (base != null) " against " + base else "")
    }

    override fun resolvePath(relativePath: String, base: String?): URL = try {
      //try the parent resolver
      val res = super.resolvePath(relativePath, base)
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

  private fun getDummyPlugins(ideRoot: File): List<IdePlugin> {
    val allXmlFiles = FileUtils.listFiles(ideRoot, WildcardFileFilter("*.xml"), TrueFileFilter.TRUE)
    val pathResolver = getFromSourcesPathResolver(allXmlFiles)
    return getDummyPlugins(allXmlFiles, pathResolver)
  }

  private fun getDummyPlugins(xmlFiles: Collection<File>, pathResolver: XIncludePathResolver): List<IdePlugin> = xmlFiles
      .filter { "plugin.xml" == it.name }
      .map { it.absoluteFile.parentFile }
      .filter { "META-INF" == it.name && it.isDirectory && it.parentFile != null }
      .map { it.parentFile }
      .filter { it.isDirectory }
      .mapNotNull { safeCreatePlugin(it, pathResolver) }

  private fun safeCreatePlugin(pluginFile: File, pathResolver: XIncludePathResolver): IdePlugin? {
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

  /**
   * Returns a [XIncludePathResolver] that can resolve the plugin descriptors
   * and auxiliary xml files in a locally built IDE.
   */
  private fun getFromSourcesPathResolver(xmlFiles: Iterable<File>): XIncludePathResolver {
    val xmlDescriptors = hashMapOf<String, File>()
    for (xmlFile in xmlFiles) {
      val pathParts = xmlFile.absolutePath
          .split("/")
          .dropLastWhile { it.isEmpty() }

      /**
       * Take only the last two parts of the .xml file path. For example,
       * `/home/used/Documents/ultimate/out/plugins/some/plugin/META-INF/someFile.xml`
       * will be converted to `/META-INF/someFile.xml`
       */
      if (pathParts.size >= 2) {
        val xmlFilePath = "/" + pathParts[pathParts.size - 2] + "/" + pathParts[pathParts.size - 1]
        xmlDescriptors[xmlFilePath] = xmlFile
      }
    }
    //todo: speedup the descriptors loading by caching all the /META-INF/* files.
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
    val libDir = ideaDir.resolve("lib")
    val resourcesJar = libDir.resolve("resources.jar")
    if (!resourcesJar.exists()) {
      LOG.info("IDEA CORE plugin is not found in the /lib/resources.jar")
      return null
    }

    val libFiles = libDir.listFiles().orEmpty().toList()
    val pathResolver = PluginXmlXIncludePathResolver(libFiles)
    return safeCreatePlugin(resourcesJar, pathResolver)
  }

  /**
   * Reads the plugins from the /plugins directory.
   */
  private fun readFromPluginsDir(ideaDir: File): List<IdePlugin> {
    val pluginsDir = File(ideaDir, "plugins")
    val pluginsFiles = pluginsDir.listFiles() ?: return emptyList()
    return pluginsFiles
        .filter { it.isDirectory }
        .mapNotNull { safeCreatePlugin(it, DefaultXIncludePathResolver()) }
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
