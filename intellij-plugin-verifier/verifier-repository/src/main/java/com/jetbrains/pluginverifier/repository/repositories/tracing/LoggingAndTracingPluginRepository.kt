package com.jetbrains.pluginverifier.repository.repositories.tracing

import org.jetbrains.intellij.pluginRepository.PluginDownloader
import org.jetbrains.intellij.pluginRepository.PluginManager
import org.jetbrains.intellij.pluginRepository.PluginRepository
import org.jetbrains.intellij.pluginRepository.PluginUpdateManager
import org.jetbrains.intellij.pluginRepository.model.IntellijUpdateMetadata
import org.jetbrains.intellij.pluginRepository.model.PluginBean
import org.jetbrains.intellij.pluginRepository.model.PluginId
import org.jetbrains.intellij.pluginRepository.model.PluginUpdateBean
import org.jetbrains.intellij.pluginRepository.model.PluginUserBean
import org.jetbrains.intellij.pluginRepository.model.PluginXmlBean
import org.jetbrains.intellij.pluginRepository.model.ProductEnum
import org.jetbrains.intellij.pluginRepository.model.ProductFamily
import org.jetbrains.intellij.pluginRepository.model.StringPluginId
import org.jetbrains.intellij.pluginRepository.model.UpdateBean
import org.jetbrains.intellij.pluginRepository.model.UpdateDeleteBean
import org.jetbrains.intellij.pluginRepository.model.UpdateId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

private val LOG: Logger = LoggerFactory.getLogger(LoggingAndTracingPluginRepository::class.java)

class LoggingAndTracingPluginRepository(private val delegateRepository: PluginRepository) : PluginRepository {
  override val downloader: PluginDownloader get() = LoggingAndTracingPluginDownloader(delegateRepository.downloader)
  override val pluginManager: PluginManager get() = LoggingAndTracingPluginManager(delegateRepository.pluginManager)
  override val pluginUpdateManager: PluginUpdateManager get() = LoggingAndTracingPluginUpdateManager(delegateRepository.pluginUpdateManager)
  override val uploader get() = delegateRepository.uploader
  override val vendorManager get() = delegateRepository.vendorManager

  private class LoggingAndTracingPluginManager(private val delegate: PluginManager): PluginManager by delegate {
    override fun getAllPluginsIds(): List<String> {
      LOG.debug("Retrieving all plugins identifiers")
      return delegate.getAllPluginsIds()
    }

    @Deprecated("Since IDEA 2020.2 is deprecated")
    override fun getCompatiblePluginsXmlIds(
      build: String,
      max: Int,
      offset: Int,
      query: String
    ): List<String> {
      LOG.debug("Getting compatible plugins identifiers for build '$build'. Max: $max, offset: $offset, query: $query")
      return delegate.getCompatiblePluginsXmlIds(build, max, offset, query)
    }

    override fun getPlugin(id: PluginId): PluginBean? {
      LOG.debug("Getting plugin with ID=$id")
      return delegate.getPlugin(id)
    }

    override fun getPluginByXmlId(
      xmlId: StringPluginId,
      family: ProductFamily
    ): PluginBean? {
      LOG.debug("Retrieving plugin metadata for '$xmlId'", family.name.lowercase())
      return delegate.getPluginByXmlId(
        xmlId,
        family
      )
    }

    override fun getPluginChannels(id: PluginId): List<String> {
      LOG.debug("Retrieving plugin channels for plugin '$id'")
      return delegate.getPluginChannels(id)
    }

    override fun getPluginCompatibleProducts(id: PluginId): List<ProductEnum> {
      LOG.debug("Retrieving compatible products for plugin '$id'")
      return delegate.getPluginCompatibleProducts(id)
    }

    override fun getPluginDevelopers(id: PluginId): List<PluginUserBean> {
      LOG.debug("Retrieving plugin developers for plugin '$id'")
      return delegate.getPluginDevelopers(id)
    }

    override fun getPluginLastCompatibleUpdates(
      build: String,
      xmlId: StringPluginId
    ): List<UpdateBean> {
      LOG.debug("Getting last compatible updates for plugin '$xmlId' and build '$build'")
      return delegate.getPluginLastCompatibleUpdates(build, xmlId)
    }

    override fun getPluginVersions(id: PluginId): List<UpdateBean> {
      LOG.debug("Retrieving plugin versions for plugin '$id'")
      return delegate.getPluginVersions(id).also {
        LOG.debug("Plugin '$id' has ${it.size} versions")
      }
    }

    override fun getPluginXmlIdByDependency(
      dependency: String,
      includeOptional: Boolean
    ): List<String> {
      val optionalMsg = if (includeOptional) "optional " else ""
      LOG.debug("Getting plugin XML IDs by ${optionalMsg}dependency '$dependency'")
      return delegate.getPluginXmlIdByDependency(
        dependency,
        includeOptional
      ).also {
        LOG.debug("Found ${it.size} plugins for ${optionalMsg}dependency '$dependency'")
      }
    }

    @Deprecated("Will be removed for performance reasons")
    override fun listPlugins(
      ideBuild: String,
      channel: String?,
      pluginId: StringPluginId?
    ): List<PluginXmlBean> {
      LOG.debug("Listing plugins for IDE '$ideBuild' (channel '$channel')")
      return delegate.listPlugins(ideBuild, channel, pluginId).also {
        LOG.debug("Found ${it.size} plugins for IDE '$ideBuild' (channel '$channel')")
      }
    }

    override fun searchCompatibleUpdates(
      xmlIds: List<StringPluginId>,
      build: String,
      channel: String,
      module: String
    ): List<UpdateBean> {
      val channelMsg = if (channel.isNotBlank()) ", channel '$channel'" else ""
      val specMsg = if (xmlIds.isEmpty() && module.isNotBlank()) {
        "module $module$channelMsg"
      } else {
        val moduleMsg = if (module.isNotBlank()) ", module '$module'" else ""
        val pluginIdMsg = when (xmlIds.size) {
          0 -> "no plugin IDs"
          1 -> xmlIds.first()
          else -> xmlIds.joinToString()
        }
        "$pluginIdMsg for $build $channelMsg$moduleMsg"
      }

      LOG.debug("Searching for compatible plugin updates of $specMsg")
      return delegate.searchCompatibleUpdates(
        xmlIds,
        build,
        channel,
        module
      ).also {
        LOG.debug("Found ${it.size} compatible plugin updates of $specMsg")
      }
    }
  }

  private class LoggingAndTracingPluginDownloader(private val delegate: PluginDownloader) : PluginDownloader {
    override fun download(
      xmlId: StringPluginId,
      version: String,
      targetPath: File,
      channel: String?
    ): File? {
      LOG.debug("Downloading $xmlId:$version to [$targetPath] (channel: $channel)")
      return delegate.download(xmlId, version, targetPath, channel)
    }

    override fun download(
      id: UpdateId,
      targetPath: File
    ): File? {
      LOG.debug("Downloading update (ID=$id) to [$targetPath]")
      return delegate.download(id, targetPath)
    }

    override fun downloadLatestCompatiblePlugin(
      xmlId: StringPluginId,
      ideBuild: String,
      targetPath: File,
      channel: String?
    ): File? {
      LOG.debug("Downloading latest compatible plugin '$xmlId' with IDE '$ideBuild 'to [$targetPath] (channel: $channel)")
      return delegate.downloadLatestCompatiblePlugin(xmlId, ideBuild, targetPath, channel)
    }

    override fun downloadLatestCompatiblePluginViaBlockMap(
      xmlId: StringPluginId,
      ideBuild: String,
      targetPath: File,
      oldFile: File,
      channel: String?
    ): File? {
      LOG.debug("Downloading latest compatible plugin '$xmlId' with IDE '$ideBuild 'to [$targetPath] (channel: $channel)")
      return delegate.downloadLatestCompatiblePluginViaBlockMap(xmlId, ideBuild, targetPath, oldFile, channel)
    }

    override fun downloadViaBlockMap(
      xmlId: StringPluginId,
      version: String,
      targetPath: File,
      oldFile: File,
      channel: String?
    ): File? {
      LOG.debug("Downloading $xmlId:$version to [$targetPath] (channel: $channel, blockmap download via '$targetPath)'")
      return delegate.downloadViaBlockMap(xmlId, version, targetPath, oldFile, channel)
    }

    override fun downloadViaBlockMap(
      id: UpdateId,
      targetPath: File,
      oldFile: File
    ): File? {
      LOG.debug("Downloading plugin update '$id' to [$targetPath] (blockmap download via '$oldFile)'")
      return delegate.downloadViaBlockMap(id, targetPath, oldFile)
    }

  }

  private class LoggingAndTracingPluginUpdateManager(private val delegate: PluginUpdateManager) : PluginUpdateManager {
    override fun getUpdateById(id: UpdateId): PluginUpdateBean? {
      LOG.debug("Getting plugin update '$id'")
      return delegate.getUpdateById(id)
    }

    override fun getUpdatesByVersionAndFamily(
      xmlId: StringPluginId,
      version: String,
      family: ProductFamily
    ): List<PluginUpdateBean> {
      LOG.debug("Getting plugin updates for '$xmlId:$version' (family: ${family.name.lowercase()})")
      return delegate.getUpdatesByVersionAndFamily(
        xmlId,
        version,
        family
      )
    }

    override fun deleteUpdate(updateId: UpdateId): UpdateDeleteBean? {
      LOG.debug("Deleting plugin update '$updateId'")
      return delegate.deleteUpdate(updateId)
    }

    override fun getIntellijUpdateMetadata(pluginId: PluginId, updateId: UpdateId): IntellijUpdateMetadata? {
      LOG.debug("Retrieving plugin update metadata for plugin '$pluginId' and update '$updateId'")
      return delegate.getIntellijUpdateMetadata(pluginId, updateId)
    }

    override fun getIntellijUpdateMetadataBatch(updateIds: List<Pair<PluginId, UpdateId>>): Map<UpdateId, IntellijUpdateMetadata> {
      val aggregatedUpdateIds: Map<PluginId, List<UpdateId>> = updateIds.groupBy { it.first }
        .mapValues { (_, pairs: List<Pair<PluginId, UpdateId>>) ->
          pairs.map { (_, updateId: UpdateId)  -> updateId }
        }

      val updateIdMsg = aggregatedUpdateIds.map { (plugin, updates) ->
        "$plugin (${updates.size} updates): ${updates.joinToString { it.toString() }}"
      }.joinToString("; ")

      LOG.debug("Retrieving plugin update metadata for plugins $updateIdMsg")
      return delegate.getIntellijUpdateMetadataBatch(updateIds)
    }
  }
}

fun PluginRepository.withLogging(): PluginRepository = LoggingAndTracingPluginRepository(this)