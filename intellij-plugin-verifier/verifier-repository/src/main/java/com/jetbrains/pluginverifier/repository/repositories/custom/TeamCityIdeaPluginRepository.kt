/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.repositories.custom

import com.jetbrains.plugin.structure.intellij.repository.CustomPluginRepositoryListingType
import com.jetbrains.pluginverifier.repository.PluginRepository
import java.net.URL

/**
 * [PluginRepository] of JetBrains TeamCity Plugin for IntelliJ IDEA, which is built on TeamCity.
 */
class TeamCityIdeaPluginRepository(
  private val buildServerUrl: URL,
  private val tcPluginSourceCodeUrl: URL
) : CustomPluginRepository() {

  private val defaultRepository = DefaultCustomPluginRepository(
    repositoryUrl,
    URL(buildServerUrl.toExternalForm().trimEnd('/') + "/update/idea-plugins.xml"),
    CustomPluginRepositoryListingType.SIMPLE,
    "JetBrains TeamCity Plugin Repository"
  )

  override val repositoryUrl: URL
    get() = buildServerUrl

  // Replace some values because they are not specified in /update/idea-plugins.xml
  override fun requestAllPlugins(): List<CustomPluginInfo> =
    defaultRepository.requestAllPlugins()
      .filter { it.pluginId == "Jetbrains TeamCity Plugin" }
      .map { pluginInfo ->
        CustomPluginInfo(
          pluginInfo.pluginId,
          "TeamCity Integration",
          pluginInfo.version,
          "JetBrains",
          repositoryUrl,
          pluginInfo.downloadUrl,
          pluginInfo.browserUrl,
          tcPluginSourceCodeUrl,
          pluginInfo.sinceBuild,
          pluginInfo.untilBuild
        )
      }

  override val presentableName
    get() = "JetBrains TeamCity Plugin Repository"
}