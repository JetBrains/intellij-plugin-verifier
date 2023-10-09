/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.JarFilesResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.CompiledModulesResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.CompositeResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Path

class IdeManagerImpl : IdeManager() {

  override fun createIde(idePath: Path): Ide = createIde(idePath, null)

  override fun createIde(idePath: Path, version: IdeVersion?): Ide {
    if (!idePath.isDirectory) {
      throw IOException("Specified path does not exist or is not a directory: $idePath")
    }
    val fromCompiled = isCompiledCommunity(idePath) || isCompiledUltimate(idePath)
    val fromDistribution = isDistributionIde(idePath)
    if (!fromCompiled && !fromDistribution) {
      throw InvalidIdeException(idePath, "IDE directory content is invalid")
    }

    val readIdeVersion = version ?: if (fromCompiled) {
      readVersionFromIdeSources(idePath)
    } else {
      readIdeVersionFromDistribution(idePath)
    }

    val ideVersion = if (readIdeVersion.productCode.isNotEmpty()) {
      readIdeVersion
    } else {
      //MPS builds' "build.txt" file does not specify product code.
      //MPS builds contain "build.number" file whose "build.number" key-value contains the product code.
      readIdeVersionFromBuildNumberFile(idePath) ?: readIdeVersion
    }

    val product = IntelliJPlatformProduct.fromIdeVersion(ideVersion) ?: IntelliJPlatformProduct.IDEA

    val bundledPlugins = if (fromCompiled) {
      readCompiledBundledPlugins(idePath, ideVersion)
    } else {
      readDistributionBundledPlugins(idePath, product, ideVersion)
    }

    return IdeImpl(idePath, ideVersion, bundledPlugins)
  }

  private fun readDistributionBundledPlugins(idePath: Path, product: IntelliJPlatformProduct, ideVersion: IdeVersion): List<IdePlugin> {
    val platformJarFiles = idePath.resolve("lib")
      .listFiles()
      .filter { it.isJar() }
    val platformResourceResolver = PlatformResourceResolver(platformJarFiles)
    val bundledPlugins = readBundledPlugins(idePath, platformResourceResolver, ideVersion)
    val platformPlugins = readPlatformPlugins(idePath, product, platformJarFiles, platformResourceResolver, ideVersion)
    return bundledPlugins + platformPlugins
  }

  /**
   * IDE uses platform class loader to resolve resources. That class loader includes jar files under `<ide>/lib` directory.
   * By default, for `relativePath` is resolved against jar files' roots. For example, if there are
   * `a.jar`, `b.jar`, `c.jar` files in `<ide>/lib` directory, `/META-INF/someInclude.xml`
   * will be searched by the following URLs:
   * - `jar:file:<path>/a.jar!/META-INF/someInclude.xml`
   * - `jar:file:<path>/b.jar!/META-INF/someInclude.xml`
   * - `jar:file:<path>/c.jar!/META-INF/someInclude.xml`
   *
   * But if the `relativePath` is not absolute it can still be relative to `META-INF`, not relative to jars' roots.
   *
   * Usually, `<xi:include href="<some-path>">` of bundled plugins specify either:
   * - Absolute paths to xml files that reside either in the same .jar file or in a different .jar file of the platform.
   * For example, `<xi:include href="/META-INF/include.xml">` declared in `plugin.xml` where `plugin.xml` resides in `one.jar`
   * and `include.xml` resides in `two.jar`.
   *
   * - Non-absolute paths to xml files that reside in the same .jar file as the xml containg such `<xi:include>`.
   * For example, `<xi:include href="nearby.xml">` declared in `plugin.xml` where `plugin.xml` and `nearby.xml`
   * reside in the same jar file.
   * But rarely `nearby.xml` may reside in another platform's jar file. Apparently, IDE handles such a case accidentally:
   * an ugly fallback hack is used `com.intellij.util.io.URLUtil.openResourceStream(URL)`.
   */
  private class PlatformResourceResolver(platformJarFiles: List<Path>) : ResourceResolver {
    private val jarFilesResourceResolver = JarFilesResourceResolver(platformJarFiles)

    override fun resolveResource(relativePath: String, basePath: Path): ResourceResolver.Result {
      val resolveResult = jarFilesResourceResolver.resolveResource(relativePath, basePath)
      if (resolveResult !is ResourceResolver.Result.NotFound) {
        return resolveResult
      }
      if (basePath.startsWith("META-INF")) {
        var metaInf = basePath
        while (!metaInf.endsWith("META-INF")) metaInf = metaInf.parent
        val metaInfResult = jarFilesResourceResolver.resolveResource(relativePath, metaInf)
        if (metaInfResult !is ResourceResolver.Result.NotFound) {
          return metaInfResult
        }
      }
      if (!relativePath.startsWith("/")) {
        return jarFilesResourceResolver.resolveResource("/META-INF/$relativePath", basePath)
      }
      return ResourceResolver.Result.NotFound
    }
  }

  private fun readIdeVersionFromDistribution(idePath: Path): IdeVersion {
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
          locations.joinToString { "'" + idePath.relativize(it) + "'" }
      )
    return readBuildNumber(buildTxtFile)
  }

  private fun readIdeVersionFromBuildNumberFile(idePath: Path): IdeVersion? {
    val buildNumberFile = idePath.resolve("build.number")
    if (buildNumberFile.exists()) {
      val lines = buildNumberFile.readLines()
      for (line in lines) {
        if (line.startsWith("build.number=")) {
          return IdeVersion.createIdeVersionIfValid(line.substringAfter("build.number="))
        }
      }
    }
    return null
  }


  private fun readVersionFromIdeSources(idePath: Path): IdeVersion {
    val locations = listOf(
      idePath.resolve("build.txt"),
      idePath.resolve("community").resolve("build.txt")
    )
    val buildTxtFile = locations.find { it.exists() }
      ?: throw InvalidIdeException(idePath, "Unable to find IDE version file 'build.txt' or 'community/build.txt'")
    return readBuildNumber(buildTxtFile)
  }

  private fun readBuildNumber(versionFile: Path): IdeVersion {
    val buildNumberString = versionFile.readText().trim()
    return IdeVersion.createIdeVersion(buildNumberString)
  }

  private fun readCompiledBundledPlugins(idePath: Path, ideVersion: IdeVersion): List<IdePlugin> {
    val compilationRoot = getCompiledClassesRoot(idePath)!!
    val moduleRoots = compilationRoot.listFiles().toList()
    val librariesJars = getRepositoryLibrariesJars(idePath)
    val pathResolver = CompositeResourceResolver(
      listOf(
        CompiledModulesResourceResolver(moduleRoots),
        JarFilesResourceResolver(librariesJars)
      )
    )
    return readCompiledBundledPlugins(idePath, moduleRoots, pathResolver, ideVersion)
  }

  private fun readCompiledBundledPlugins(
    idePath: Path,
    moduleRoots: List<Path>,
    pathResolver: ResourceResolver,
    ideVersion: IdeVersion
  ): List<IdePlugin> {
    val plugins = arrayListOf<IdePlugin>()
    for (moduleRoot in moduleRoots) {
      val pluginXmlFile = moduleRoot.resolve(IdePluginManager.META_INF).resolve(IdePluginManager.PLUGIN_XML)
      if (pluginXmlFile.isFile) {
        plugins += createBundledPluginExceptionally(idePath, moduleRoot, pathResolver, IdePluginManager.PLUGIN_XML, ideVersion)
      }
    }
    return plugins
  }

  private fun readPlatformPlugins(
    idePath: Path,
    product: IntelliJPlatformProduct,
    jarFiles: List<Path>,
    platformResourceResolver: ResourceResolver,
    ideVersion: IdeVersion
  ): List<IdePlugin> {
    val platformPlugins = arrayListOf<IdePlugin>()
    val descriptorPaths = listOf(product.platformPrefix + "Plugin.xml", IdePluginManager.PLUGIN_XML, PLATFORM_PLUGIN_XML)

    for (jarFile in jarFiles) {
      val descriptorPath = FileSystems.newFileSystem(jarFile, IdeManagerImpl::class.java.classLoader).use { jarFs ->
        descriptorPaths.find { jarFs.getPath(IdePluginManager.META_INF).resolve(it).exists() }
      }
      if (descriptorPath != null) {
        platformPlugins += createBundledPluginExceptionally(idePath, jarFile, platformResourceResolver, descriptorPath, ideVersion)
      }
    }

    if (platformPlugins.none { it.pluginId == "com.intellij" }) {
      throw InvalidIdeException(idePath, "Platform plugins are not found. They must be declared in one of ${descriptorPaths.joinToString()}")
    }

    return platformPlugins
  }

  private fun readBundledPlugins(
    idePath: Path,
    platformResourceResolver: ResourceResolver,
    ideVersion: IdeVersion
  ): List<IdePlugin> {
    return idePath
      .resolve("plugins")
      .listFiles()
      .filter { it.isDirectory }
      .mapNotNull { readBundledPlugin(idePath, it, platformResourceResolver, ideVersion) }
  }

  private fun readBundledPlugin(
    idePath: Path,
    pluginFile: Path,
    pathResolver: ResourceResolver,
    ideVersion: IdeVersion
  ): IdePlugin? = try {
    createBundledPluginExceptionally(idePath, pluginFile, pathResolver, IdePluginManager.PLUGIN_XML, ideVersion)
  } catch (e: InvalidIdeException) {
    LOG.warn("Failed to read bundled plugin '${idePath.relativize(pluginFile)}': ${e.reason}")
    null
  }

  private fun createBundledPluginExceptionally(
    idePath: Path,
    pluginFile: Path,
    pathResolver: ResourceResolver,
    descriptorPath: String,
    ideVersion: IdeVersion
  ): IdePlugin = when (val creationResult = IdePluginManager
    .createManager(pathResolver)
    .createBundledPlugin(pluginFile, ideVersion, descriptorPath)
    ) {
    is PluginCreationSuccess -> creationResult.plugin
    is PluginCreationFail -> throw InvalidIdeException(
      idePath,
      "Plugin '${idePath.relativize(pluginFile)}' is invalid: " +
        creationResult.errorsAndWarnings.filter { it.level == PluginProblem.Level.ERROR }.joinToString { it.message }
    )
  }

  companion object {

    private val LOG = LoggerFactory.getLogger(IdeManagerImpl::class.java)

    // in idea-IE com.intellij plugin is defined in PlatformLangPlugin.xml file
    private const val PLATFORM_PLUGIN_XML = "PlatformLangPlugin.xml"

    fun isCompiledUltimate(ideaDir: Path) = getCompiledClassesRoot(ideaDir) != null &&
      ideaDir.resolve(".idea").isDirectory &&
      ideaDir.resolve("community").resolve(".idea").isDirectory

    fun isCompiledCommunity(ideaDir: Path) = getCompiledClassesRoot(ideaDir) != null &&
      ideaDir.resolve(".idea").isDirectory &&
      !ideaDir.resolve("community").resolve(".idea").isDirectory

    fun isDistributionIde(ideaDir: Path) = ideaDir.resolve("lib").isDirectory &&
      !ideaDir.resolve(".idea").isDirectory

    fun getCompiledClassesRoot(ideaDir: Path): Path? =
      listOf(
        ideaDir.resolve("out").resolve("production"),
        ideaDir.resolve("out").resolve("classes").resolve("production"),
        ideaDir.resolve("out").resolve("compilation").resolve("classes").resolve("production")
      ).find { it.isDirectory }
  }

}
