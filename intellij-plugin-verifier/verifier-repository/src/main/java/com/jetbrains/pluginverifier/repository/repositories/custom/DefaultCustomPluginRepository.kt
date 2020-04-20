/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.repositories.custom

import com.jetbrains.pluginverifier.misc.createOkHttpClient
import com.jetbrains.pluginverifier.network.executeSuccessfully
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import java.net.URL
import java.util.concurrent.TimeUnit

class DefaultCustomPluginRepository(
  override val repositoryUrl: URL,
  private val pluginsListXmlUrl: URL,
  private val pluginsXmlListingType: CustomPluginRepositoryListingType,
  override val presentableName: String
) : CustomPluginRepository() {

  private val repositoryConnector = Retrofit.Builder()
    .baseUrl("http://not-used.com")
    .client(createOkHttpClient(false, 5, TimeUnit.MINUTES))
    .build()
    .create(PluginListConnector::class.java)

  public override fun requestAllPlugins(): List<CustomPluginInfo> {
    val xmlContent = repositoryConnector.getPluginsListXml(pluginsListXmlUrl.toExternalForm())
      .executeSuccessfully()
      .body()!!
      .string()
    return CustomPluginRepositoryListingParser.parseListOfPlugins(
      xmlContent,
      pluginsListXmlUrl,
      repositoryUrl,
      pluginsXmlListingType
    )
  }

  override fun toString() = presentableName

  private interface PluginListConnector {
    @GET
    fun getPluginsListXml(@Url url: String): Call<ResponseBody>
  }

}