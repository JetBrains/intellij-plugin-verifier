/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.repositories.custom

import com.jetbrains.plugin.structure.intellij.repository.CustomPluginRepositoryListingParser
import com.jetbrains.plugin.structure.intellij.repository.CustomPluginRepositoryListingType
import com.jetbrains.pluginverifier.misc.RestApiFailed
import com.jetbrains.pluginverifier.misc.RestApiOk
import com.jetbrains.pluginverifier.misc.RestApis
import java.net.URL

class DefaultCustomPluginRepository(
  override val repositoryUrl: URL,
  private val pluginsListXmlUrl: URL,
  private val pluginsXmlListingType: CustomPluginRepositoryListingType,
  override val presentableName: String
) : CustomPluginRepository() {

  private val repositoryConnector by lazy {
    PluginListConnector(pluginsListXmlUrl)
  }

  public override fun requestAllPlugins(): List<CustomPluginInfo> {
    val xmlContent = repositoryConnector.getPluginsListXml()
    return CustomPluginRepositoryListingParser.parseListOfPlugins(
      xmlContent,
      pluginsListXmlUrl,
      repositoryUrl,
      pluginsXmlListingType
    ).map {
      CustomPluginInfo(
        it.pluginId,
        it.pluginName,
        it.version,
        it.vendor,
        it.repositoryUrl,
        it.downloadUrl,
        it.browserUrl,
        it.sourceCodeUrl,
        it.sinceBuild,
        it.untilBuild
      )
    }
  }

  override fun toString() = presentableName

  private class PluginListConnector(private val endpointUrl: URL) {
    private val restApis = RestApis()

    fun getPluginsListXml(): XmlString {
      return when (val apiResult = restApis.getRawString(endpointUrl.toExternalForm())) {
        is RestApiOk<String> -> apiResult.payload
        is RestApiFailed<*> -> "<plugin-repository />"
      }
    }
  }
}

typealias XmlString = String


