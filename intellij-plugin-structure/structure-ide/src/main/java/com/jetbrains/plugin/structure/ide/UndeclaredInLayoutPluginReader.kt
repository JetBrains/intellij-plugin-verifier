/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.ide.IdeManagerImpl.PlatformResourceResolver
import com.jetbrains.plugin.structure.ide.plugin.DefaultPluginIdProvider
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.plugin.BundledPluginManager
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.PluginArtifactPath
import com.jetbrains.plugin.structure.intellij.problems.AnyProblemToWarningPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG: Logger = LoggerFactory.getLogger(UndeclaredInLayoutPluginReader::class.java)

class UndeclaredInLayoutPluginReader(private val supportedProductCodes: Set<String>) : ProductInfoBasedIdeManager.PluginReader {
  private val pluginIdProvider = DefaultPluginIdProvider()

  private val bundledPluginManager = BundledPluginManager(pluginIdProvider)

  override fun readPlugins(idePath: Path, productInfo: ProductInfo, ideVersion: IdeVersion): List<IdePlugin> {
    if (!supports(productInfo)) return emptyList()

    val resourceResolver = PlatformResourceResolver.of(idePath)

    val identifiersInPluginsDir = bundledPluginManager.getBundledPluginIds(idePath)
    val identifiersInLayout = productInfo.layout.map { it.name }

    return identifiersInPluginsDir
      .filterNotIn(identifiersInLayout)
      .mapNotNull {
        try {
          createBundledPlugin(idePath, it.path, resourceResolver, ideVersion)
        } catch (e: InvalidIdeException) {
          // TODO layout issues log level should be configurable
          LOG.debug(e.reason)
          null
        }
      }
  }

  private fun supports(product: ProductInfo): Boolean = product.productCode in supportedProductCodes

  private fun Set<PluginArtifactPath>.filterNotIn(layoutIdentifiers: List<String>): List<PluginArtifactPath> {
    return filterNot { it.pluginId in layoutIdentifiers }
  }

  @Throws(InvalidIdeException::class)
  private fun createBundledPlugin(
    idePath: Path,
    pluginFile: Path,
    resourceResolver: ResourceResolver,
    ideVersion: IdeVersion
  ): IdePlugin = when (val creationResult = IdePluginManager
    .createManager(resourceResolver)
    // TODO consolidate bundled plugin construction across multiple invocations
    .createBundledPlugin(
      pluginFile,
      ideVersion,
      PLUGIN_XML,
      problemResolver = AnyProblemToWarningPluginCreationResultResolver
    )
  ) {
    is PluginCreationSuccess -> creationResult.plugin
    is PluginCreationFail -> throw InvalidIdeException(
      idePath,
      "Plugin '${idePath.relativize(pluginFile)}' is invalid: " +
        creationResult.errorsAndWarnings.filter { it.level == PluginProblem.Level.ERROR }.joinToString { it.message }
    )
  }
}