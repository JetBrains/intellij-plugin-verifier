/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.ide.layout.LayoutComponentNameSource
import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode
import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode.SKIP_AND_WARN
import com.jetbrains.plugin.structure.ide.layout.PluginMetadataSource
import com.jetbrains.plugin.structure.ide.resolver.ValidatingLayoutComponentsProvider
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.platform.ProductInfoParseException
import com.jetbrains.plugin.structure.intellij.platform.ProductInfoParser
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import java.io.IOException
import java.nio.file.Path

internal const val PRODUCT_INFO_JSON = "product-info.json"
internal const val MACOS_RESOURCES_DIRECTORY = "Resources"
internal val VERSION_FROM_PRODUCT_INFO: IdeVersion? = null

class ProductInfoBasedIdeManager(
  missingLayoutFileMode: MissingLayoutFileMode = SKIP_AND_WARN,
  private val additionalProductInfoPluginReader: PluginReader<PluginMetadataSource.ProductInfoSource> = NoOpProductInfoPluginReader,
  private val additionalLayoutComponentsPluginReader: PluginReader<PluginMetadataSource.LayoutComponentsSource> = NoOpLayoutComponentsPluginReader
) : IdeManager() {

  private val productInfoParser = ProductInfoParser()

  private val layoutComponentsProvider = ValidatingLayoutComponentsProvider(missingLayoutFileMode)

  @Throws(InvalidIdeException::class)
  override fun createIde(idePath: Path): Ide = createIde(idePath, VERSION_FROM_PRODUCT_INFO)

  override fun createIde(idePath: Path, version: IdeVersion?): Ide {
    assertProductInfoPresent(idePath)
    try {
      val productInfo = productInfoParser.parse(idePath.productInfoJson!!)
      val ideVersion = version ?: createIdeVersion(productInfo)
      return createIde(idePath, ideVersion, productInfo)
    } catch (e: ProductInfoParseException) {
      throw InvalidIdeException(idePath, e)
    }
  }

  private fun createIde(idePath: Path, ideVersion: IdeVersion, productInfo: ProductInfo): Ide {
    if (!idePath.isDirectory) {
      throw IOException("Specified path does not exist or is not a directory: $idePath")
    }
    val layoutComponents = layoutComponentsProvider.resolveLayoutComponents(productInfo, idePath)

    val pluginCollectionProviders = mutableListOf<PluginCollectionProvider<Path>>()
    pluginCollectionProviders += ProductInfoLayoutBasedPluginCollectionProvider(
      additionalLayoutComponentsPluginReader,
      SingletonCachingJarFileSystemProvider,
    )
    if (additionalProductInfoPluginReader !is NoOpProductInfoPluginReader) {
      pluginCollectionProviders += ProductInfoPluginReaderPluginCollectionProvider(
        additionalProductInfoPluginReader)
    }

    val pluginCollectionProvider = CompositePluginCollectionProvider.of(pluginCollectionProviders)
    return ProductInfoBasedIde.of(idePath, ideVersion, productInfo, layoutComponents, pluginCollectionProvider)
  }

  private fun createIdeVersion(productInfo: ProductInfo): IdeVersion {
    val versionString = buildString {
      with(productInfo) {
        if (productCode.isNotEmpty()) append(productCode).append("-")
        append(buildNumber)
      }
    }
    return IdeVersion.createIdeVersion(versionString)
  }

  private val Path.productInfoJson: Path?
    get() {
      val locations = listOf<Path>(
        resolve(PRODUCT_INFO_JSON),
        resolve(MACOS_RESOURCES_DIRECTORY).resolve(PRODUCT_INFO_JSON)
      )
      return locations.firstOrNull { it.exists() }
    }

  @Throws(InvalidIdeException::class)
  private fun assertProductInfoPresent(idePath: Path): Path {
    return idePath.productInfoJson ?: throw InvalidIdeException(idePath, "The '$PRODUCT_INFO_JSON' file is not available.")
  }

  fun supports(idePath: Path): Boolean = idePath.productInfoJson != null
    && isAtLeastVersion(idePath, "242")

  private fun isAtLeastVersion(idePath: Path, expectedVersion: String): Boolean {
    return when (val version = BuildTxtIdeVersionProvider().readIdeVersion(idePath)) {
      is IdeVersionResolution.Found -> version.ideVersion > IdeVersion.createIdeVersion(expectedVersion)
      is IdeVersionResolution.Failed,
      is IdeVersionResolution.NotFound -> false
    }
  }

  interface PluginReader<S : PluginMetadataSource> {
    fun readPlugins(
      idePath: Path,
      pluginMetadataSource: S,
      layoutComponentNameSource: LayoutComponentNameSource<S>,
      ideVersion: IdeVersion
    ): List<IdePlugin>

    fun supports(pluginMetadataSource: PluginMetadataSource): Boolean
  }

  private object NoOpProductInfoPluginReader : PluginReader<PluginMetadataSource.ProductInfoSource> {
    override fun readPlugins(
      idePath: Path,
      pluginMetadataSource: PluginMetadataSource.ProductInfoSource,
      layoutComponentNameSource: LayoutComponentNameSource<PluginMetadataSource.ProductInfoSource>,
      ideVersion: IdeVersion
    ): List<IdePlugin> = emptyList()

    override fun supports(pluginMetadataSource: PluginMetadataSource): Boolean {
      return true
    }
  }

  private object NoOpLayoutComponentsPluginReader : PluginReader<PluginMetadataSource.LayoutComponentsSource> {
    override fun readPlugins(
      idePath: Path,
      pluginMetadataSource: PluginMetadataSource.LayoutComponentsSource,
      layoutComponentNameSource: LayoutComponentNameSource<PluginMetadataSource.LayoutComponentsSource>,
      ideVersion: IdeVersion
    ): List<IdePlugin> = emptyList()

    override fun supports(pluginMetadataSource: PluginMetadataSource): Boolean {
      return true
    }
  }
}
