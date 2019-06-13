package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.base.utils.isZip
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.PluginXmlXIncludePathResolver
import com.jetbrains.plugin.structure.intellij.utils.ThreeState
import com.jetbrains.plugin.structure.intellij.utils.URLUtil
import com.jetbrains.plugin.structure.intellij.utils.xincludes.DefaultXIncludePathResolver
import com.jetbrains.plugin.structure.intellij.utils.xincludes.XIncludeException
import com.jetbrains.plugin.structure.intellij.utils.xincludes.XIncludePathResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.io.File
import java.io.IOException
import java.net.URL

class IdeManagerImpl : IdeManager() {

  override fun createIde(idePath: File): Ide = createIde(idePath, null)

  override fun createIde(idePath: File, version: IdeVersion?): Ide {
    if (!idePath.isDirectory) {
      throw IOException("Specified path does not exist or is not a directory: $idePath")
    }
    val fromCompiled = isCompiledCommunity(idePath) || isCompiledUltimate(idePath)
    val fromDistribution = isDistributionIde(idePath)
    if (!fromCompiled && !fromDistribution) {
      throw InvalidIdeException(idePath, "IDE directory content is invalid")
    }

    val ideVersion = version ?: if (fromCompiled) {
      readVersionFromIdeSources(idePath)
    } else {
      readIdeVersionFromDistribution(idePath)
    }

    val bundledPlugins = if (fromCompiled) {
      readCompiledBundledPlugins(idePath)
    } else {
      readDistributionBundledPlugins(idePath, ideVersion)
    }

    return IdeImpl(idePath, ideVersion, bundledPlugins)
  }

  private fun readDistributionBundledPlugins(idePath: File, ideVersion: IdeVersion): List<IdePlugin> {
    val jarFiles = idePath.resolve("lib").listFiles().orEmpty().filter { it.isJar() || it.isZip() }
    val pathResolver = PluginXmlXIncludePathResolver(jarFiles)
    val product = IntelliJPlatformProduct.fromIdeVersion(ideVersion) ?: IntelliJPlatformProduct.IDEA

    val bundledPlugins = readBundledPlugins(idePath, ideVersion, pathResolver)
    val platformPlugins = readPlatformPlugins(pathResolver, jarFiles, product, idePath)

    if (platformPlugins.none { it.pluginId == "com.intellij" }) {
      throw InvalidIdeException(idePath, "Platform plugins are not found")
    }
    return bundledPlugins + platformPlugins
  }

  private fun readIdeVersionFromDistribution(idePath: File): IdeVersion {
    val locations = listOf(
        idePath.resolve("build.txt"),
        idePath.resolve("Resources").resolve("build.txt"),
        idePath.resolve("community").resolve("build.txt"),
        idePath.resolve("ultimate").resolve("community").resolve("build.txt")
    )
    val buildTxtFile = locations.find { it.exists() }
        ?: throw InvalidIdeException(
            idePath,
            "Build number is not found in the following files relative to $idePath: " +
                locations.joinToString { "'" + it.relativeTo(idePath) + "'" }
        )
    return readBuildNumber(buildTxtFile)
  }

  private fun readVersionFromIdeSources(idePath: File): IdeVersion {
    val locations = listOf(
        idePath.resolve("build.txt"),
        idePath.resolve("community").resolve("build.txt")
    )
    val buildTxtFile = locations.find { it.exists() }
        ?: throw InvalidIdeException(idePath, "Unable to find IDE version file 'build.txt' or 'community/build.txt'")
    return readBuildNumber(buildTxtFile)
  }

  private class BundledPluginFromSourceXIncludePathResolver(private val moduleRoots: Array<out File>) : XIncludePathResolver {

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

  private fun readCompiledBundledPlugins(idePath: File): List<IdePlugin> {
    val compilationRoot = getCompiledClassesRoot(idePath)!!
    val moduleRoots = compilationRoot.listFiles().orEmpty()
    val pathResolver = BundledPluginFromSourceXIncludePathResolver(moduleRoots)
    return readCompiledBundledPlugins(idePath, moduleRoots, pathResolver)
  }

  private fun readCompiledBundledPlugins(idePath: File, moduleRoots: Array<out File>, pathResolver: XIncludePathResolver): List<IdePlugin> {
    val plugins = arrayListOf<IdePlugin>()
    for (moduleRoot in moduleRoots) {
      val pluginXmlFile = moduleRoot.resolve(IdePluginManager.META_INF).resolve(IdePluginManager.PLUGIN_XML)
      if (pluginXmlFile.isFile) {
        plugins += createPluginExceptionally(idePath, moduleRoot, pathResolver, IdePluginManager.PLUGIN_XML)
      }
    }
    return plugins
  }

  private fun readPlatformPlugins(
      pathResolver: XIncludePathResolver,
      jarFiles: List<File>,
      product: IntelliJPlatformProduct,
      idePath: File
  ): List<IdePlugin> {
    val plugins = arrayListOf<IdePlugin>()
    val descriptorPaths = listOf(IdePluginManager.PLUGIN_XML, product.platformPrefix + "Plugin.xml")

    for (jarFile in jarFiles) {
      for (descriptorPath in descriptorPaths) {
        val descriptorUrl = URLUtil.getJarEntryURL(jarFile, "${IdePluginManager.META_INF}/$descriptorPath")
        if (URLUtil.resourceExists(descriptorUrl) == ThreeState.YES) {
          plugins += createPluginExceptionally(idePath, jarFile, pathResolver, descriptorPath)
        }
      }
    }

    return plugins
  }

  private fun readBundledPlugins(idePath: File, ideVersion: IdeVersion, pathResolver: XIncludePathResolver): List<IdePlugin> {
    val pluginsFiles = idePath.resolve("plugins").listFiles().orEmpty()
    return pluginsFiles
        .filter { it.isDirectory }
        .mapNotNull { readBundledPlugin(idePath, ideVersion, it, pathResolver) }
  }

  private fun readBundledPlugin(idePath: File, ideVersion: IdeVersion, pluginFile: File, pathResolver: XIncludePathResolver): IdePlugin? {
    try {
      return createPluginExceptionally(idePath, pluginFile, pathResolver, IdePluginManager.PLUGIN_XML)
    } catch (e: InvalidIdeException) {
      /*
        Bundled plugin "duplicates", which resides in <ide>/plugins/duplicates, is incorrect in IDEs prior to 173.
        It does not have "plugin.xml" but only "duplicates.xml" and thus it is not fully correct standalone IDE plugin.
        This is not the case for recent IDEs.
      */
      if (pluginFile.isDirectory && pluginFile.relativeTo(idePath) == File("plugins/duplicates") && ideVersion.baselineVersion < 173) {
        return null
      }
      throw e
    }
  }

  private fun createPluginExceptionally(
      idePath: File,
      pluginFile: File,
      pathResolver: XIncludePathResolver,
      descriptorPath: String,
      validateDescriptor: Boolean = false
  ): IdePlugin = when (val creationResult = IdePluginManager
      .createManager(pathResolver)
      .createPlugin(pluginFile, validateDescriptor, descriptorPath)
    ) {
    is PluginCreationSuccess -> creationResult.plugin
    is PluginCreationFail -> throw InvalidIdeException(
        idePath,
        "Plugin '${pluginFile.relativeTo(idePath)}' is invalid: " +
            creationResult.errorsAndWarnings.filter { it.level == PluginProblem.Level.ERROR }.joinToString { it.message }
    )
  }

  companion object {

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
