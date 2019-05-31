package com.jetbrains.plugin.structure.ide

import com.google.common.base.Joiner
import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.base.utils.isZip
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.PluginXmlXIncludePathResolver
import com.jetbrains.plugin.structure.intellij.utils.ThreeState
import com.jetbrains.plugin.structure.intellij.utils.URLUtil
import com.jetbrains.plugin.structure.intellij.utils.xincludes.DefaultXIncludePathResolver
import com.jetbrains.plugin.structure.intellij.utils.xincludes.XIncludeException
import com.jetbrains.plugin.structure.intellij.utils.xincludes.XIncludePathResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL

class IdeManagerImpl : IdeManager() {

  override fun createIde(ideDir: File): Ide = createIde(ideDir, null)

  override fun createIde(idePath: File, version: IdeVersion?): Ide {
    check(idePath.exists()) { "IDE file doesn't exist: $idePath" }
    val ideVersion = version ?: when {
      isCompiledCommunity(idePath) || isCompiledUltimate(idePath) -> readVersionFromIdeSources(idePath)
      isDistributionIde(idePath) -> readIdeVersionFromDistribution(idePath)
      else -> throw IllegalArgumentException("Invalid IDE: $idePath")
    }

    val bundledPlugins = when {
      isCompiledCommunity(idePath) || isCompiledUltimate(idePath) -> readCompiledPlugins(getCompiledClassesRoot(idePath)!!)
      isDistributionIde(idePath) -> {
        val jarFiles = idePath.resolve("lib").listFiles().orEmpty().filter { it.isJar() || it.isZip() }
        val pathResolver = PluginXmlXIncludePathResolver(jarFiles)
        val product = IntelliJPlatformProduct.fromIdeVersion(ideVersion) ?: IntelliJPlatformProduct.IDEA

        val bundledPlugins = readBundledPlugins(idePath, pathResolver)
        val platformPlugins = readPlatformPlugins(pathResolver, jarFiles, product)

        if (platformPlugins.none { it.pluginName == SPECIAL_PLUGIN_NAME }) {
          LOG.warn("Platform plugin '$SPECIAL_PLUGIN_NAME' is not found")
        }
        bundledPlugins + platformPlugins
      }
      else -> throw IllegalArgumentException()
    }

    return IdeImpl(idePath, ideVersion, bundledPlugins)
  }

  private fun readIdeVersionFromDistribution(idePath: File): IdeVersion {
    val locations = listOf(
        idePath.resolve("build.txt"),
        idePath.resolve("Resources").resolve("build.txt"),
        idePath.resolve("community").resolve("build.txt"),
        idePath.resolve("ultimate").resolve("community").resolve("build.txt")
    )
    val buildTxtFile = locations.find { it.exists() }
        ?: throw IllegalArgumentException(
            "Build number is not found in the following files relative to $idePath: " +
                locations.map { it.relativeTo(idePath) }.joinToString()
        )
    return readBuildNumber(buildTxtFile)
  }

  private fun readVersionFromIdeSources(idePath: File): IdeVersion {
    val locations = listOf(
        idePath.resolve("build.txt"),
        idePath.resolve("community").resolve("build.txt")
    )
    val buildTxtFile = locations.find { it.exists() }
        ?: throw IllegalArgumentException("Unable to find IDE version file (build.txt or community/build.txt)")
    return readBuildNumber(buildTxtFile)
  }

  private class PluginFromSourceXIncludePathResolver(private val moduleRoots: Array<File>) : XIncludePathResolver {

    private val defaultResolver = DefaultXIncludePathResolver()

    override fun resolvePath(relativePath: String, base: String?): URL {
      try {
        val url = defaultResolver.resolvePath(relativePath, base)
        if (URLUtil.resourceExists(url) == ThreeState.YES) {
          return url
        }
      } catch (ignored: Exception) {
      }

      //Try to resolve path against module roots. [base] is ignored.

      val adjustedPath = when {
        relativePath.startsWith("./") -> "/META-INF/" + relativePath.substringAfter("./")
        relativePath.startsWith("/") -> relativePath.substringAfter("/")
        else -> relativePath
      }

      for (moduleRoot in moduleRoots) {
        val file = moduleRoot.resolve(adjustedPath)
        if (file.exists()) {
          return URLUtil.fileToUrl(file)
        }
      }
      throw XIncludeException("Unable to resolve $relativePath against $base in ${moduleRoots.size} module roots")
    }
  }

  private fun readBuildNumber(versionFile: File): IdeVersion {
    val buildNumberString = versionFile.readText().trim()
    return IdeVersion.createIdeVersion(buildNumberString)
  }

  private fun readCompiledPlugins(compilationRoot: File): List<IdePlugin> {
    val moduleRoots = compilationRoot.listFiles()
    val pathResolver = PluginFromSourceXIncludePathResolver(moduleRoots)
    return readCompiledPlugins(moduleRoots, pathResolver)
  }

  private fun readCompiledPlugins(moduleRoots: Array<File>, pathResolver: XIncludePathResolver): List<IdePlugin> {
    val plugins = arrayListOf<IdePlugin>()
    for (moduleRoot in moduleRoots) {
      val pluginXmlFile = moduleRoot.resolve(IdePluginManager.META_INF).resolve(IdePluginManager.PLUGIN_XML)
      if (pluginXmlFile.isFile) {
        val plugin = safeCreatePlugin(moduleRoot, pathResolver, IdePluginManager.PLUGIN_XML)
        if (plugin != null) {
          plugins += plugin
        }
      }
    }
    return plugins
  }

  private fun safeCreatePlugin(pluginFile: File, pathResolver: XIncludePathResolver, descriptorPath: String): IdePlugin? {
    try {
      return when (val creationResult = IdePluginManager.createManager(pathResolver).createPlugin(pluginFile, false, descriptorPath)) {
        is PluginCreationSuccess -> creationResult.plugin
        is PluginCreationFail -> {
          val problems = creationResult.errorsAndWarnings
          LOG.warn("Failed to read plugin " + pluginFile + ". Problems: " + Joiner.on(", ").join(problems))
          null
        }
      }
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      LOG.warn("Unable to create plugin from sources: $pluginFile", e)
    }

    return null
  }

  private fun readPlatformPlugins(
      pathResolver: XIncludePathResolver,
      jarFiles: List<File>,
      product: IntelliJPlatformProduct
  ): List<IdePlugin> {
    val plugins = arrayListOf<IdePlugin>()

    for (libFile in jarFiles) {
      for (descriptorPath in listOf(IdePluginManager.PLUGIN_XML, product.platformPrefix + "Plugin.xml")) {
        val descriptorUrl = URLUtil.getJarEntryURL(libFile, "${IdePluginManager.META_INF}/$descriptorPath")
        if (URLUtil.resourceExists(descriptorUrl) == ThreeState.YES) {
          val plugin = readPluginFromUrl(descriptorUrl, descriptorPath, pathResolver)
          if (plugin != null) {
            plugins.add(plugin)
          }
        }
      }
    }

    return plugins
  }

  private fun readPluginFromUrl(
      descriptorUrl: URL,
      descriptorPath: String,
      pathResolver: XIncludePathResolver
  ): IdePlugin? {
    when {
      URLUtil.FILE_PROTOCOL == descriptorUrl.protocol -> {
        val descriptorFile = URLUtil.urlToFile(descriptorUrl)
        val pathname = descriptorFile.path.replace('\\', '/').substringBeforeLast(descriptorPath)
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
  private fun readBundledPlugins(ideaDir: File, pathResolver: XIncludePathResolver): List<IdePlugin> {
    val pluginsFiles = ideaDir.resolve("plugins").listFiles().orEmpty()
    return pluginsFiles
        .filter { it.isDirectory }
        .mapNotNull { safeCreatePlugin(it, pathResolver, IdePluginManager.PLUGIN_XML) }
  }

  companion object {

    private val LOG = LoggerFactory.getLogger(IdeManagerImpl::class.java)

    private const val SPECIAL_PLUGIN_NAME = "IDEA CORE"

    fun isCompiledUltimate(ideaDir: File) = getCompiledClassesRoot(ideaDir) != null &&
        ideaDir.resolve(".idea").isDirectory &&
        ideaDir.resolve("community").resolve(".idea").isDirectory

    fun isCompiledCommunity(ideaDir: File) = getCompiledClassesRoot(ideaDir) != null &&
        ideaDir.resolve(".idea").isDirectory &&
        !ideaDir.resolve("community").resolve(".idea").isDirectory

    fun isDistributionIde(ideaDir: File) = ideaDir.resolve("lib").isDirectory &&
        !ideaDir.resolve(".idea").isDirectory

    fun getCompiledClassesRoot(ideaDir: File): File? =
        listOf(
            ideaDir.resolve("out").resolve("production"),
            ideaDir.resolve("out").resolve("classes").resolve("production"),
            ideaDir.resolve("out").resolve("compilation").resolve("classes").resolve("production")
        ).find { it.isDirectory }
  }

}
