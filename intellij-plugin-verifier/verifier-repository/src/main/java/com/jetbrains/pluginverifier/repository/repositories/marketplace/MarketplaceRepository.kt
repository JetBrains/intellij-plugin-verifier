package com.jetbrains.pluginverifier.repository.repositories.marketplace

import com.google.common.collect.ImmutableMap
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginRepository
import org.jetbrains.intellij.pluginRepository.PluginRepositoryFactory
import org.jetbrains.intellij.pluginRepository.model.IntellijUpdateMetadata
import java.net.MalformedURLException
import java.net.URL

class MarketplaceRepository(val repositoryURL: URL) : PluginRepository {

  private val pluginRepositoryInstance = PluginRepositoryFactory.create(host = repositoryURL.toExternalForm())

  override fun getLastCompatiblePlugins(ideVersion: IdeVersion): List<UpdateInfo> {
    val pluginManager = pluginRepositoryInstance.pluginManager
    val buildNumber = ideVersion.asString()
    val result = arrayListOf<UpdateInfo>()
    var offset = 0
    val max = 10000
    while (true) {
      val compatiblePluginsXmlIds = pluginManager.getCompatiblePluginsXmlIds(buildNumber, max, offset)
      val compatibleUpdates = pluginManager.searchCompatibleUpdates(compatiblePluginsXmlIds, buildNumber)
      for (compatibleUpdate in compatibleUpdates) {
        val updateInfo = requestAndCreateUpdateInfo(compatibleUpdate.pluginId, compatibleUpdate.id)
        if (updateInfo != null) {
          result += updateInfo
        }
      }
      if (compatibleUpdates.isEmpty()) {
        break
      }
      offset += max
    }
    return result
  }

  override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? {
    val compatibleUpdates = pluginRepositoryInstance.pluginManager.searchCompatibleUpdates(listOf(pluginId), ideVersion.asString())
    val compatibleUpdate = compatibleUpdates.firstOrNull() ?: return null
    return requestAndCreateUpdateInfo(compatibleUpdate.pluginId, compatibleUpdate.id)
  }

  override fun getAllVersionsOfPlugin(pluginId: String): List<UpdateInfo> {
    TODO("No way to implement it currently.")
  }

  override fun getIdOfPluginDeclaringModule(moduleId: String): String? =
    INTELLIJ_MODULE_TO_CONTAINING_PLUGIN[moduleId]

  private fun requestAndCreateUpdateInfo(pluginId: Int, updateId: Int): UpdateInfo? {
    val updateMetadata = pluginRepositoryInstance.pluginUpdateManager.getIntellijUpdateMetadata(pluginId, updateId)
    return if (updateMetadata != null) createUpdateInfo(updateMetadata, pluginId, updateId) else null
  }

  private fun createUpdateInfo(
    updateMetadata: IntellijUpdateMetadata,
    pluginId: Int,
    updateId: Int
  ) = UpdateInfo(
    updateMetadata.xmlId,
    updateMetadata.name,
    updateMetadata.version,
    updateMetadata.since.prepareIdeVersion(),
    updateMetadata.until.prepareIdeVersion(),
    updateMetadata.vendor,
    parseSourceCodeUrl(updateMetadata.sourceCodeUrl),
    getBrowserUrl(pluginId),
    updateId,
    getDownloadUrl(updateId),
    updateMetadata.tags
  )

  /**
   * Given the [update ID] [updateId], which is a unique identifier
   * of the plugin version in the Plugins Repository database,
   * returns its more detailed [UpdateInfo].
   */
  fun getPluginInfoById(updateId: Int): UpdateInfo? {
    val pluginUpdateBean = pluginRepositoryInstance.pluginUpdateManager.getUpdateById(updateId) ?: return null
    return requestAndCreateUpdateInfo(pluginUpdateBean.pluginId, updateId)
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

  private companion object {
    /**
     * TODO: implement this mapping on the Plugins Repository: MP-1152.
     * Currently, the Plugin Repository doesn't know about what modules are declared in what plugins.
     *
     * The list of IntelliJ plugins which define some modules
     * (e.g. the plugin "org.jetbrains.plugins.ruby" defines a module "com.intellij.modules.ruby")
     */
    private val INTELLIJ_MODULE_TO_CONTAINING_PLUGIN = ImmutableMap.of(
      "com.intellij.modules.ruby", "org.jetbrains.plugins.ruby",
      "com.intellij.modules.php", "com.jetbrains.php",
      "com.intellij.modules.python", "Pythonid",
      "com.intellij.modules.swift.lang", "com.intellij.clion-swift"
    )
  }
}