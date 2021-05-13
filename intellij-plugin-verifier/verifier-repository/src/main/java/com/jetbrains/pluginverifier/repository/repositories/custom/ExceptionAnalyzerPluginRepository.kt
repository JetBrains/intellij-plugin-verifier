/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.repositories.custom

import com.jetbrains.plugin.structure.intellij.repository.CustomPluginRepositoryListingType
import com.jetbrains.pluginverifier.repository.PluginRepository
import java.net.URL

/**
 * [PluginRepository] of Exception Analyzer plugin for IntelliJ IDEA.
 */
class ExceptionAnalyzerPluginRepository(
  override val repositoryUrl: URL,
  private val eaSourceCodeUrl: URL
) : CustomPluginRepository() {

  private val defaultRepository = DefaultCustomPluginRepository(
    repositoryUrl,
    URL(repositoryUrl.toExternalForm().trimEnd('/') + "/plugins.xml"),
    CustomPluginRepositoryListingType.SIMPLE,
    "ExceptionAnalyzer Plugin Repository: ${repositoryUrl.toExternalForm()}"
  )

  // Replace some values because they are not specified in /plugins.xml.
  override fun requestAllPlugins(): List<CustomPluginInfo> =
    defaultRepository.requestAllPlugins()
      .filter { it.pluginId == "com.intellij.sisyphus" }
      .map { pluginInfo ->
        CustomPluginInfo(
          pluginInfo.pluginId,
          "ExceptionAnalyzer",
          pluginInfo.version,
          "JetBrains",
          repositoryUrl,
          pluginInfo.downloadUrl,
          pluginInfo.browserUrl,
          eaSourceCodeUrl,
          pluginInfo.sinceBuild,
          pluginInfo.untilBuild
        )
      }

  override val presentableName
    get() = defaultRepository.presentableName

}