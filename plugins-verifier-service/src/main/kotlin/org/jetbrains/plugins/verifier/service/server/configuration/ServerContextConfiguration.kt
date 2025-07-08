/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.server.configuration

import com.jetbrains.plugin.structure.base.utils.createDir
import com.jetbrains.plugin.structure.base.utils.deleteLogged
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.ide.repositories.IdeRepository
import com.jetbrains.pluginverifier.plugin.DefaultPluginDetailsProvider
import com.jetbrains.pluginverifier.plugin.PluginFilesBank
import com.jetbrains.pluginverifier.plugin.SizeLimitedPluginDetailsCache
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import org.jetbrains.plugins.verifier.service.database.MapDbServerDatabase
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.server.ServiceDAO
import org.jetbrains.plugins.verifier.service.server.configuration.properties.TaskManagerProperties
import org.jetbrains.plugins.verifier.service.service.features.FeatureExtractorService
import org.jetbrains.plugins.verifier.service.service.features.FeatureServiceProtocol
import org.jetbrains.plugins.verifier.service.service.verifier.VerificationResultFilter
import org.jetbrains.plugins.verifier.service.service.verifier.VerifierService
import org.jetbrains.plugins.verifier.service.service.verifier.VerifierServiceProtocol
import org.jetbrains.plugins.verifier.service.setting.DiskUsageDistributionSetting
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path
import java.nio.file.Paths

@Configuration
class ServerContextConfiguration(
  @Value("\${verifier.service.max.disk.space.mb}") maxDiskSpace: Long
) {
  companion object {
    private val LOG = LoggerFactory.getLogger(ServerContextConfiguration::class.java)

    private const val PLUGIN_DETAILS_CACHE_SIZE = 30

    private const val IDE_DESCRIPTORS_CACHE_SIZE = 10
  }

  @Bean
  fun serverContext(
    buildProperties: BuildProperties,
    ideRepository: IdeRepository,
    pluginRepository: MarketplaceRepository,
    featureServiceProtocol: FeatureServiceProtocol,
    @Value("\${verifier.service.jdk.8.dir}") defaultJdkPath: Path,
    @Value("\${verifier.service.home.directory}") applicationHomeDir: String,
    @Value("\${verifier.service.clear.corrupted.database}") clearDatabaseOnCorruption: Boolean
  ): ServerContext {
    LOG.info("Server is ready to start")

    val applicationHomeDirPath = Paths.get(applicationHomeDir)
    applicationHomeDirPath.createDir()
    val loadedPluginsDir = applicationHomeDirPath.resolve("loaded-plugins").createDir()
    val extractedPluginsDir = applicationHomeDirPath.resolve("extracted-plugins").createDir()
    val ideFilesDir = applicationHomeDirPath.resolve("ides").createDir()

    val pluginDownloadDirSpaceSetting = getPluginDownloadDirDiskSpaceSetting()

    val archiveManager = PluginArchiveManager(extractedPluginsDir)
    val pluginDetailsProvider = DefaultPluginDetailsProvider(archiveManager)
    val pluginFilesBank = PluginFilesBank.create(pluginRepository, loadedPluginsDir, pluginDownloadDirSpaceSetting)
    val pluginDetailsCache = SizeLimitedPluginDetailsCache(PLUGIN_DETAILS_CACHE_SIZE, pluginFilesBank, pluginDetailsProvider)

    val ideDownloadDirDiskSpaceSetting = getIdeDownloadDirDiskSpaceSetting()
    val serviceDAO = openServiceDAO(applicationHomeDirPath, clearDatabaseOnCorruption)

    val ideFilesBank = IdeFilesBank(ideFilesDir, ideRepository, ideDownloadDirDiskSpaceSetting)
    val ideDescriptorsCache = IdeDescriptorsCache(IDE_DESCRIPTORS_CACHE_SIZE, ideFilesBank, defaultJdkPath)

    val verificationResultsFilter = VerificationResultFilter()

    return ServerContext(
      buildProperties.version,
      ideRepository,
      ideFilesBank,
      pluginRepository,
      serviceDAO,
      ideDescriptorsCache,
      pluginFilesBank,
      pluginDetailsCache,
      verificationResultsFilter
    )
  }

  @Bean
  fun verifierService(
    serverContext: ServerContext,
    verifierServiceProtocol: VerifierServiceProtocol,
    taskManager: TaskManager,
    taskManagerProperties: TaskManagerProperties,
    @Value("\${verifier.service.enable.plugin.verifier.service}") enableService: Boolean,
    @Value("\${verifier.service.scheduler.period.seconds}") period: Long,
    @Value("\${verifier.service.verifier.min.ide}") verifierMinIdeVersion: String
  ): VerifierService {
    val verifierService = with(serverContext) {
      VerifierService(
        taskManager,
        verifierServiceProtocol,
        pluginDetailsCache,
        ideDescriptorsCache,
        verificationResultsFilter,
        pluginRepository,
        serviceDAO,
        IdeVersion.createIdeVersion(verifierMinIdeVersion),
        period
      )
    }
    if (enableService) {
      verifierService.start()
    }
    return verifierService
  }

  @Bean
  fun featureService(
    serverContext: ServerContext,
    featureServiceProtocol: FeatureServiceProtocol,
    taskManager: TaskManager,
    @Value("\${verifier.service.enable.feature.extractor.service}") enableService: Boolean,
    @Value("\${verifier.service.feature.extractor.ide.build}") featureExtractorIdeVersion: String
  ): FeatureExtractorService {
    val featureService = with(serverContext) {
      FeatureExtractorService(
        taskManager,
        featureServiceProtocol,
        ideDescriptorsCache,
        pluginDetailsCache,
        ideRepository,
        IdeVersion.createIdeVersion(featureExtractorIdeVersion)
      )
    }
    if (enableService) {
      featureService.start()
    }
    return featureService
  }

  private fun openServiceDAO(applicationHomeDir: Path, clearDatabaseOnCorruption: Boolean): ServiceDAO {
    val databasePath = applicationHomeDir.resolve("database")
    try {
      return createServiceDAO(databasePath)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      LOG.error("Unable to open/create database", e)
      LOG.info("Flag to clear database on corruption is " + if (clearDatabaseOnCorruption) "ON" else "OFF")
      if (clearDatabaseOnCorruption) {
        LOG.info("Trying to recreate database")
        databasePath.deleteLogged()
        try {
          val recreatedDAO = createServiceDAO(databasePath)
          LOG.info("Successfully recreated database")
          return recreatedDAO
        } catch (e: Exception) {
          e.rethrowIfInterrupted()
          LOG.error("Fatal error creating database: ${e.message}", e)
          throw e
        }
      }
      LOG.error("Do not clear database. Abort.")
      throw e
    }
  }

  private fun createServiceDAO(databasePath: Path): ServiceDAO {
    return ServiceDAO(MapDbServerDatabase(databasePath))
  }

  private val maxDiskSpaceUsage = SpaceAmount.ofMegabytes(maxDiskSpace.coerceAtLeast(10000))

  private fun getIdeDownloadDirDiskSpaceSetting() =
    DiskSpaceSetting(DiskUsageDistributionSetting.IDE_DOWNLOAD_DIR.getIntendedSpace(maxDiskSpaceUsage))

  private fun getPluginDownloadDirDiskSpaceSetting() =
    DiskSpaceSetting(DiskUsageDistributionSetting.PLUGIN_DOWNLOAD_DIR.getIntendedSpace(maxDiskSpaceUsage))
}