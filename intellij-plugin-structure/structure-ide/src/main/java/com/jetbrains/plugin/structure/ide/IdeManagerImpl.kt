package com.jetbrains.plugin.structure.ide

import com.google.common.base.Joiner
import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager.*
import com.jetbrains.plugin.structure.intellij.plugin.PluginXmlXIncludePathResolver
import com.jetbrains.plugin.structure.intellij.utils.StringUtil
import com.jetbrains.plugin.structure.intellij.utils.ThreeState
import com.jetbrains.plugin.structure.intellij.utils.URLUtil
import com.jetbrains.plugin.structure.intellij.utils.xincludes.DefaultXIncludePathResolver
import com.jetbrains.plugin.structure.intellij.utils.xincludes.XIncludeException
import com.jetbrains.plugin.structure.intellij.utils.xincludes.XIncludePathResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.io.filefilter.WildcardFileFilter
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.URL

class IdeManagerImpl : IdeManager() {

  override fun createIde(ideDir: File): Ide = createIde(ideDir, null)

  override fun createIde(idePath: File, version: IdeVersion?): Ide {
    if (!idePath.exists()) {
      throw IllegalArgumentException("IDE file $idePath is not found")
    }
    val ideVersion = version ?: if (isSourceDir(idePath)) {
      readVersionFromSourcesDir(idePath)
    } else {
      readVersionFromBinaries(idePath)
    }
    val bundled = if (isSourceDir(idePath)) {
      getDummyPluginsFromSources(idePath)
    } else {
      readBundledPlugins(idePath) + readPlatformPlugins(idePath, ideVersion)
    }
    return IdeImpl(idePath, ideVersion, bundled)
  }

  private fun readVersionFromBinaries(idePath: File): IdeVersion {
    val suitableFiles = listOf(
        idePath.resolve("build.txt"),
        idePath.resolve("Resources").resolve("build.txt"),
        idePath.resolve("community").resolve("build.txt"),
        idePath.resolve("ultimate").resolve("community").resolve("build.txt")
    )
    val firstExists = suitableFiles.find { it.exists() }
    if (firstExists != null) {
      return readBuildNumber(firstExists)
    }
    throw IllegalArgumentException("Build number is not found in the following files relative to $idePath: " +
        suitableFiles.map { it.relativeTo(idePath) }.joinToString()
    )
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
      throw XIncludeException("Unable to resolve " + normalizedPath + if (base != null) " against $base" else "")
    }

    override fun resolvePath(relativePath: String, base: String?) = try {
      //try the parent resolver
      val res = super.resolvePath(relativePath, base)
      URLUtil.openStream(res)
      res
    } catch (e: IOException) {
      resolveOutputDirectories(relativePath, base)
    }
  }

  private fun readBuildNumber(versionFile: File): IdeVersion {
    val buildNumberString = versionFile.readText().trim()
    return IdeVersion.createIdeVersion(buildNumberString)
  }

  private fun getDummyPluginsFromSources(ideaDir: File): List<IdePlugin> = when {
    isUltimate(ideaDir) -> getDummyPlugins(getUltimateClassesRoot(ideaDir)!!)
    isCommunity(ideaDir) -> getDummyPlugins(getCommunityClassesRoot(ideaDir)!!)
    else -> throw IllegalArgumentException("Incorrect IDEA structure: $ideaDir. It must be Community or Ultimate sources root with compiled class files.")
  }

  private fun getDummyPlugins(ideRoot: File): List<IdePlugin> {
    val allXmlFiles = FileUtils.listFiles(ideRoot, WildcardFileFilter("*.xml"), TrueFileFilter.TRUE)
    val pathResolver = getFromSourcesPathResolver(allXmlFiles)
    return getDummyPlugins(allXmlFiles, pathResolver)
  }

  private fun getDummyPlugins(xmlFiles: Collection<File>, pathResolver: XIncludePathResolver) =
      xmlFiles
          .asSequence()
          .filter { "plugin.xml" == it.name }
          .map { it.absoluteFile.parentFile }
          .filter { "META-INF" == it.name && it.isDirectory && it.parentFile != null }
          .map { it.parentFile }
          .filter { it.isDirectory }
          .mapNotNull { safeCreatePlugin(it, pathResolver, PLUGIN_XML) }
          .toList()

  private fun safeCreatePlugin(
      pluginFile: File,
      pathResolver: XIncludePathResolver,
      descriptorPath: String
  ): IdePlugin? {
    try {
      val creationResult = createManager(pathResolver).createPlugin(pluginFile, false, descriptorPath)
      return when (creationResult) {
        is PluginCreationSuccess -> creationResult.plugin
        is PluginCreationFail -> {
          val problems = creationResult.errorsAndWarnings
          LOG.warn("Failed to read plugin " + pluginFile + ". Problems: " + Joiner.on(", ").join(problems))
          null
        }
      }
    } catch (e: Exception) {
      LOG.warn("Unable to create plugin from sources: $pluginFile", e)
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
    return PluginFromSourceXIncludePathResolver(xmlDescriptors)
  }

  private fun readPlatformPlugins(ideaDir: File, ideVersion: IdeVersion): List<IdePlugin> {
    val libFiles = ideaDir.resolve("lib").listFiles().orEmpty().toList()

    val plugins = arrayListOf<IdePlugin>()

    val pathResolver = PluginXmlXIncludePathResolver(libFiles)

    val product = IntelliJPlatformProduct.fromIdeVersion(ideVersion) ?: IntelliJPlatformProduct.IDEA
    val platformDescriptorPath = product.platformPrefix + "Plugin.xml"
    val descriptorPaths = listOf(PLUGIN_XML, platformDescriptorPath)

    for (libFile in libFiles) {
      if (libFile.isJar()) {
        for (descriptorPath in descriptorPaths) {
          val descriptorUrl = URLUtil.getJarEntryURL(libFile, "$META_INF/$descriptorPath")
          if (URLUtil.resourceExists(descriptorUrl) == ThreeState.YES) {
            val plugin = readPluginFromUrl(descriptorUrl, descriptorPath, pathResolver)
            if (plugin != null) {
              plugins.add(plugin)
            }
          }
        }
      }
    }

    if (plugins.none { it.pluginName == SPECIAL_PLUGIN_NAME }) {
      LOG.warn("Platform plugin '$SPECIAL_PLUGIN_NAME' is not found in $ideaDir")
    }

    return plugins
  }

  private fun readPluginFromUrl(
      descriptorUrl: URL,
      descriptorPath: String,
      pathResolver: PluginXmlXIncludePathResolver
  ): IdePlugin? {
    when {
      URLUtil.FILE_PROTOCOL == descriptorUrl.protocol -> {
        val descriptorFile = URLUtil.urlToFile(descriptorUrl)
        val pathname = StringUtil.toSystemIndependentName(descriptorFile.path).substringBeforeLast(descriptorPath)
        val pluginDir = File(pathname).parentFile
        return safeCreatePlugin(pluginDir, pathResolver, descriptorPath)
      }
      URLUtil.JAR_PROTOCOL == descriptorUrl.protocol -> {
        val path = descriptorUrl.file
        val pluginJar = URLUtil.urlToFile(URL(path.substringBefore(URLUtil.JAR_SEPARATOR)))
        return safeCreatePlugin(pluginJar, pathResolver, descriptorPath)
      }
      else -> LOG.warn("Cannot load plugin from '$descriptorUrl'. Unknown URL protocol.")
    }
    return null
  }

  /**
   * Reads plugins from the /plugins directory.
   */
  private fun readBundledPlugins(ideaDir: File): List<IdePlugin> {
    val pluginsFiles = ideaDir.resolve("plugins").listFiles().orEmpty()
    return pluginsFiles
        .filter { it.isDirectory }
        .mapNotNull { safeCreatePlugin(it, DefaultXIncludePathResolver(), PLUGIN_XML) }
  }

  companion object {

    private val LOG = LoggerFactory.getLogger(IdeManagerImpl::class.java)

    private const val SPECIAL_PLUGIN_NAME = "IDEA CORE"

    fun isSourceDir(dir: File) = File(dir, ".idea").isDirectory

    fun isUltimate(ideaDir: File): Boolean = File(ideaDir, "community/.idea").isDirectory && getUltimateClassesRoot(ideaDir) != null

    fun isCommunity(ideaDir: File): Boolean = File(ideaDir, ".idea").isDirectory && getCommunityClassesRoot(ideaDir) != null

    fun getUltimateClassesRoot(ideaDir: File): File? {
      return listOf(
          ideaDir.resolve("out/classes/production"),
          ideaDir.resolve("out/compilation/classes/production")
      ).first { it.isDirectory }
    }

    fun getCommunityClassesRoot(ideaDir: File): File? {
      return ideaDir.resolve("out/production").takeIf { it.isDirectory }
    }

  }

}
