/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.listFiles
import com.jetbrains.plugin.structure.base.utils.listJars
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.resources.JarsResourceResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Path

class IdeManagerImpl : AbstractIdeManager() {
  private val jarFileSystemProvider = SingletonCachingJarFileSystemProvider

  override fun createIde(idePath: Path): Ide = createIde(idePath, null)

  override fun createIde(idePath: Path, version: IdeVersion?): Ide {
    if (!idePath.isDirectory) {
      throw IOException("Specified path does not exist or is not a directory: $idePath")
    }
    if (!isDistributionIde(idePath)) {
      throw InvalidIdeException(idePath, "IDE directory content is invalid")
    }

    val readIdeVersion = version ?: readIdeVersion(idePath)
    val ideVersion = resolveProductSpecificVersion(idePath, readIdeVersion)

    val product = IntelliJPlatformProduct.fromIdeVersion(ideVersion) ?: IntelliJPlatformProduct.IDEA

    val bundledPlugins = readBundledPlugins(idePath, product, ideVersion)
    return IdeImpl(idePath, ideVersion, bundledPlugins)
  }

  private fun readBundledPlugins(idePath: Path, product: IntelliJPlatformProduct, ideVersion: IdeVersion): List<IdePlugin> {
    val platformResourceResolver = PlatformResourceResolver.of(idePath, jarFileSystemProvider)
    val bundledPlugins = readBundledPlugins(idePath, platformResourceResolver, ideVersion)
    val platformPlugins = readPlatformPlugins(idePath, product, platformResourceResolver.platformJarFiles, platformResourceResolver, ideVersion)
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
   * - Non-absolute paths to xml files that reside in the same .jar file as the xml containing such `<xi:include>`.
   * For example, `<xi:include href="nearby.xml">` declared in `plugin.xml` where `plugin.xml` and `nearby.xml`
   * reside in the same jar file.
   * But rarely `nearby.xml` may reside in another platform's jar file. Apparently, IDE handles such a case accidentally:
   * an ugly fallback hack is used `com.intellij.util.io.URLUtil.openResourceStream(URL)`.
   */
  internal class PlatformResourceResolver(
    val platformJarFiles: List<Path>,
    platformModuleJarFiles: List<Path>,
    jarFileSystemProvider: JarFileSystemProvider
  ) : ResourceResolver {
    private val jarFilesResourceResolver = JarsResourceResolver(platformJarFiles + platformModuleJarFiles, jarFileSystemProvider)

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

    companion object {
      fun of(idePath: Path, jarFileSystemProvider: JarFileSystemProvider = SingletonCachingJarFileSystemProvider): PlatformResourceResolver {
        val platformJarFiles = idePath.resolve("lib").listJars()
        val platformModuleJarFiles = idePath.resolve("lib").resolve("modules").listJars()
        return PlatformResourceResolver(platformJarFiles, platformModuleJarFiles, jarFileSystemProvider)
      }
    }
  }

  private fun readIdeVersion(idePath: Path): IdeVersion {
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

  private fun readPlatformPlugins(
    idePath: Path,
    product: IntelliJPlatformProduct,
    jarFiles: List<Path>,
    platformResourceResolver: ResourceResolver,
    ideVersion: IdeVersion
  ): List<IdePlugin> {
    val platformPlugins = arrayListOf<IdePlugin>()
    val descriptorPaths = listOf(IdePluginManager.PLUGIN_XML, product.platformPrefix + "Plugin.xml", PLATFORM_PLUGIN_XML)

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

  companion object {

    private val LOG = LoggerFactory.getLogger(IdeManagerImpl::class.java)

    // in idea-IE com.intellij plugin is defined in PlatformLangPlugin.xml file
    internal const val PLATFORM_PLUGIN_XML = "PlatformLangPlugin.xml"

    fun isDistributionIde(ideaDir: Path) = ideaDir.resolve("lib").isDirectory &&
      !ideaDir.resolve(".idea").isDirectory

  }

}
