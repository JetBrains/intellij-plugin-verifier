/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.repositories.marketplace

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginRepository
import org.jetbrains.intellij.pluginRepository.PluginRepositoryFactory
import org.jetbrains.intellij.pluginRepository.model.IntellijUpdateMetadata
import org.jetbrains.intellij.pluginRepository.model.PluginId
import org.jetbrains.intellij.pluginRepository.model.UpdateId
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class MarketplaceRepository(val repositoryURL: URL = DEFAULT_URL) : PluginRepository {

  private val pluginRepositoryInstance = PluginRepositoryFactory.create(host = repositoryURL.toExternalForm())

  //This mapping never changes. Updates in JetBrains Marketplace have constant plugin ID.
  private val updateIdToPluginIdMapping = ConcurrentHashMap<Int, Int>()

  private val metadataCache: LoadingCache<Pair<PluginId, UpdateId>, Optional<UpdateInfo>> = CacheBuilder.newBuilder()
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .build(object : CacheLoader<Pair<PluginId, UpdateId>, Optional<UpdateInfo>>() {
      override fun load(key: Pair<PluginId, UpdateId>): Optional<UpdateInfo> {
        //Loading is required => this key is outdated => will request in batch and put to the cache.
        return Optional.empty()
      }
    })

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion): List<UpdateInfo> =
    getLastCompatiblePlugins(ideVersion, "")

  fun getLastCompatiblePlugins(ideVersion: IdeVersion, channel: String): List<UpdateInfo> {
    val pluginManager = pluginRepositoryInstance.pluginManager
    @Suppress("DEPRECATION")
    val pluginsXmlIds = pluginManager.getCompatiblePluginsXmlIds(ideVersion.asString(), MAX_AVAILABLE_PLUGINS_IN_REPOSITORY, 0)
    val updates = pluginManager.searchCompatibleUpdates(pluginsXmlIds, ideVersion.asString(), channel)
    val pluginIdAndUpdateIds = updates.map { it.pluginId to it.id }
    return getPluginInfosForManyPluginIdsAndUpdateIds(pluginIdAndUpdateIds).values.toList()
  }

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? {
    val compatibleUpdates = pluginRepositoryInstance.pluginManager.searchCompatibleUpdates(listOf(pluginId), ideVersion.asString())
    val compatibleUpdate = compatibleUpdates.firstOrNull() ?: return null
    return getOrRequestInfo(compatibleUpdate.pluginId, compatibleUpdate.id)
  }

  override fun getAllVersionsOfPlugin(pluginId: String): List<UpdateInfo> {
    val pluginBean = pluginRepositoryInstance.pluginManager.getPluginByXmlId(pluginId) ?: return emptyList()
    val pluginVersions = pluginRepositoryInstance.pluginManager.getPluginVersions(pluginBean.id)
    val pluginIdAndUpdateIds = pluginVersions.map { pluginBean.id to it.id }
    return getPluginInfosForManyPluginIdsAndUpdateIds(pluginIdAndUpdateIds).values.toList()
  }

  override fun getPluginsDeclaringModule(moduleId: String, ideVersion: IdeVersion?): List<UpdateInfo> {
    val plugins = pluginRepositoryInstance.pluginManager.searchCompatibleUpdates(
      module = moduleId, build = ideVersion?.asString().orEmpty()
    )
    val pluginIdAndUpdateIds = plugins.map { it.pluginId to it.id }
    return getPluginInfosForManyPluginIdsAndUpdateIds(pluginIdAndUpdateIds).values.toList()
  }

  private fun createAndCacheUpdateInfo(metadata: IntellijUpdateMetadata, pluginId: Int): UpdateInfo {
    val updateInfo = UpdateInfo(
      metadata.xmlId,
      metadata.name,
      metadata.version,
      metadata.since.prepareIdeVersion(),
      metadata.until.prepareIdeVersion(),
      metadata.vendor,
      parseSourceCodeUrl(metadata.sourceCodeUrl),
      getDownloadUrl(metadata.id),
      metadata.id,
      getBrowserUrl(pluginId),
      metadata.tags,
      pluginId
    )
    updateIdToPluginIdMapping[updateInfo.updateId] = pluginId
    metadataCache.put(updateInfo.pluginIntId to updateInfo.updateId, Optional.of(updateInfo))
    return updateInfo
  }

  private fun getPluginIntIdByUpdateId(updateId: Int): Int? {
    updateIdToPluginIdMapping[updateId]?.let { return it }

    val pluginUpdateBean = pluginRepositoryInstance.pluginUpdateManager.getUpdateById(updateId) ?: return null
    val pluginId = pluginUpdateBean.pluginId
    updateIdToPluginIdMapping[updateId] = pluginId
    return pluginId
  }

  fun getPluginInfoByUpdateId(updateId: Int): UpdateInfo? {
    val pluginId = getPluginIntIdByUpdateId(updateId) ?: return null
    return getOrRequestInfo(pluginId, updateId)
  }

  private fun getCachedInfo(pluginId: Int, updateId: Int): UpdateInfo? {
    val optional = metadataCache[pluginId to updateId]
    if (optional.isPresent) {
      //Return up-to-date metadata.
      return optional.get()
    }
    return null
  }

  private fun getOrRequestInfo(pluginId: Int, updateId: Int): UpdateInfo? {
    val cachedInfo = getCachedInfo(pluginId, updateId)
    if (cachedInfo != null) {
      return cachedInfo
    }
    val updateMetadata = pluginRepositoryInstance.pluginUpdateManager.getIntellijUpdateMetadata(pluginId, updateId)
      ?: return null
    return createAndCacheUpdateInfo(updateMetadata, pluginId)
  }

  @Suppress("unused") //Used in API Watcher.
  fun getPluginInfosForManyUpdateIds(updateIds: List<Int>): Map<Int, UpdateInfo> {
    val pluginAndUpdateIds = arrayListOf<Pair<PluginId, UpdateId>>()
    for (updateId in updateIds) {
      val pluginId = getPluginIntIdByUpdateId(updateId)
      if (pluginId != null) {
        pluginAndUpdateIds += pluginId to updateId
      }
    }
    return getPluginInfosForManyPluginIdsAndUpdateIds(pluginAndUpdateIds)
  }

  fun getPluginChannels(pluginId: String): List<String> {
    val pluginBean = pluginRepositoryInstance.pluginManager.getPluginByXmlId(pluginId) ?: return emptyList()
    return pluginRepositoryInstance.pluginManager.getPluginChannels(pluginBean.id)
  }

  fun getPluginInfosForManyPluginIdsAndUpdateIds(pluginAndUpdateIds: List<Pair<Int, Int>>): Map<Int, UpdateInfo> {
    val toRequest = arrayListOf<Pair<PluginId, UpdateId>>()
    val result = hashMapOf<UpdateId, UpdateInfo>()
    for ((pluginId, updateId) in pluginAndUpdateIds) {
      val cachedInfo = getCachedInfo(pluginId, updateId)
      if (cachedInfo != null) {
        result[updateId] = cachedInfo
      } else {
        toRequest += pluginId to updateId
      }
    }
    if (toRequest.isNotEmpty()) {
      val metadataBatch = pluginRepositoryInstance.pluginUpdateManager.getIntellijUpdateMetadataBatch(toRequest)
      val updateIdToPluginId = toRequest.associateBy({ it.second }, { it.first })
      for ((updateId, metadata) in metadataBatch) {
        val pluginId = updateIdToPluginId.getValue(updateId)
        result[updateId] = createAndCacheUpdateInfo(metadata, pluginId)
      }
    }
    return result
  }

  private fun getBrowserUrl(pluginId: Int) =
    URL("${repositoryURL.toExternalForm().trimEnd('/')}/plugin/$pluginId")

  private fun getDownloadUrl(updateId: Int) =
    URL("${repositoryURL.toExternalForm().trimEnd('/')}/plugin/download/?noStatistic=true&updateId=$updateId")

  private fun String?.prepareIdeVersion(): IdeVersion? =
    if (this == null || this == "" || this == "0.0") {
      null
    } else {
      IdeVersion.createIdeVersionIfValid(this)
    }

  private fun parseSourceCodeUrl(url: String?): URL? {
    if (url.isNullOrBlank()) {
      return null
    }
    return try {
      URL(url)
    } catch (e: MalformedURLException) {
      null
    }
  }

  override val presentableName
    get() = "JetBrains Marketplace ${repositoryURL.toExternalForm()}"

  override fun toString() = presentableName

  private companion object {

    private val DEFAULT_URL = URL("https://plugins.jetbrains.com")

    //In the late future this will need to be updated. Currently, there are ~= 4000 plugins in the repository available.
    // This magic constant is the limit of the Elastic Search used in the Plugin Search Service.
    // Contact JetBrains Marketplace team for details.
    private const val MAX_AVAILABLE_PLUGINS_IN_REPOSITORY = 10000
  }
}
