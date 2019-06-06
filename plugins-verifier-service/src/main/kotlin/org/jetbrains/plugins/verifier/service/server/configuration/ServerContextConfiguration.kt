package org.jetbrains.plugins.verifier.service.server.configuration

import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.ide.repositories.IdeRepository
import com.jetbrains.pluginverifier.misc.createDir
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsProviderImpl
import com.jetbrains.pluginverifier.plugin.PluginFilesBank
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import org.jetbrains.plugins.verifier.service.database.MapDbServerDatabase
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.server.ServiceDAO
import org.jetbrains.plugins.verifier.service.service.features.FeatureExtractorService
import org.jetbrains.plugins.verifier.service.service.features.FeatureServiceProtocol
import org.jetbrains.plugins.verifier.service.service.ide.AvailableIdeProtocol
import org.jetbrains.plugins.verifier.service.service.ide.AvailableIdeService
import org.jetbrains.plugins.verifier.service.service.verifier.VerificationResultFilter
import org.jetbrains.plugins.verifier.service.service.verifier.VerifierService
import org.jetbrains.plugins.verifier.service.service.verifier.VerifierServiceProtocol
import org.jetbrains.plugins.verifier.service.setting.AuthorizationData
import org.jetbrains.plugins.verifier.service.setting.DiskUsageDistributionSetting
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.tasks.TaskManagerImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path

@Configuration
class ServerContextConfiguration {
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
      availableIdeProtocol: AvailableIdeProtocol,
      featureServiceProtocol: FeatureServiceProtocol
  ): ServerContext {
    LOG.info("Server is ready to start")

    validateSystemProperties()
    serverContext = createServerContext(buildProperties.version, ideRepository, pluginRepository)

    with(serverContext) {
      addFeatureService(featureServiceProtocol)
      addAvailableIdeService(availableIdeProtocol)
    }
    return serverContext
  }

  @Bean
  fun verifierService(
      serverContext: ServerContext,
      verifierServiceProtocol: VerifierServiceProtocol,
      @Value("\${verifier.service.jdk.8.dir}") jdkPath: Path,
      @Value("\${verifier.service.enable.plugin.verifier.service}") enableService: Boolean
  ): VerifierService {
    val verifierService = with(serverContext) {
      VerifierService(
          taskManager,
          jdkDescriptorsCache,
          verifierServiceProtocol,
          pluginDetailsCache,
          ideDescriptorsCache,
          jdkPath,
          verificationResultsFilter,
          pluginRepository,
          serviceDAO
      )
    }
    if (enableService) {
      verifierService.start()
    }
    serverContext.addService(verifierService)
    return verifierService
  }

  private lateinit var serverContext: ServerContext

  private fun createServerContext(
      appVersion: String?,
      ideRepository: IdeRepository,
      pluginRepository: MarketplaceRepository
  ): ServerContext {
    val applicationHomeDir = Settings.APP_HOME_DIRECTORY.getAsPath().createDir()
    val loadedPluginsDir = applicationHomeDir.resolve("loaded-plugins").createDir()
    val extractedPluginsDir = applicationHomeDir.resolve("extracted-plugins").createDir()
    val ideFilesDir = applicationHomeDir.resolve("ides").createDir()

    val pluginDownloadDirSpaceSetting = getPluginDownloadDirDiskSpaceSetting()

    val pluginDetailsProvider = PluginDetailsProviderImpl(extractedPluginsDir)
    val pluginFilesBank = PluginFilesBank.create(pluginRepository, loadedPluginsDir, pluginDownloadDirSpaceSetting)
    val pluginDetailsCache = PluginDetailsCache(PLUGIN_DETAILS_CACHE_SIZE, pluginFilesBank, pluginDetailsProvider)
    val taskManager = TaskManagerImpl(Settings.TASK_MANAGER_CONCURRENCY.getAsInt())

    val authorizationData = AuthorizationData(Settings.SERVICE_ADMIN_PASSWORD.get())

    val jdkDescriptorsCache = JdkDescriptorsCache()

    val ideDownloadDirDiskSpaceSetting = getIdeDownloadDirDiskSpaceSetting()
    val serviceDAO = openServiceDAO(applicationHomeDir)

    val ideFilesBank = IdeFilesBank(ideFilesDir, ideRepository, ideDownloadDirDiskSpaceSetting)
    val ideDescriptorsCache = IdeDescriptorsCache(IDE_DESCRIPTORS_CACHE_SIZE, ideFilesBank)

    val verificationResultsFilter = VerificationResultFilter()

    return ServerContext(
        appVersion,
        ideRepository,
        ideFilesBank,
        pluginRepository,
        taskManager,
        authorizationData,
        jdkDescriptorsCache,
        Settings.values().toList(),
        serviceDAO,
        ideDescriptorsCache,
        pluginDetailsCache,
        verificationResultsFilter
    )
  }

  private fun openServiceDAO(applicationHomeDir: Path): ServiceDAO {
    val databasePath = applicationHomeDir.resolve("database")
    try {
      return createServiceDAO(databasePath)
    } catch (e: Exception) {
      e.rethrowIfInterrupted()
      LOG.error("Unable to open/create database", e)
      LOG.info("Flag to clear database on corruption is " + if (Settings.CLEAR_DATABASE_ON_CORRUPTION.getAsBoolean()) "ON" else "OFF")
      if (Settings.CLEAR_DATABASE_ON_CORRUPTION.getAsBoolean()) {
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

  private val maxDiskSpaceUsage = SpaceAmount.ofMegabytes(Settings.MAX_DISK_SPACE_MB.getAsLong().coerceAtLeast(10000))

  private fun getIdeDownloadDirDiskSpaceSetting() =
      DiskSpaceSetting(DiskUsageDistributionSetting.IDE_DOWNLOAD_DIR.getIntendedSpace(maxDiskSpaceUsage))

  private fun getPluginDownloadDirDiskSpaceSetting() =
      DiskSpaceSetting(DiskUsageDistributionSetting.PLUGIN_DOWNLOAD_DIR.getIntendedSpace(maxDiskSpaceUsage))

  private fun ServerContext.addFeatureService(featureServiceProtocol: FeatureServiceProtocol) {
    val featureService = FeatureExtractorService(
        taskManager,
        featureServiceProtocol,
        ideDescriptorsCache,
        pluginDetailsCache,
        ideRepository
    )
    addService(featureService)
    if (Settings.ENABLE_FEATURE_EXTRACTOR_SERVICE.getAsBoolean()) {
      featureService.start()
    }
  }

  private fun ServerContext.addAvailableIdeService(availableIdeProtocol: AvailableIdeProtocol) {
    val availableIdeService = AvailableIdeService(
        taskManager,
        availableIdeProtocol,
        ideRepository
    )
    addService(availableIdeService)
    if (Settings.ENABLE_AVAILABLE_IDE_SERVICE.getAsBoolean()) {
      availableIdeService.start()
    }
  }

  private fun validateSystemProperties() {
    LOG.info("Validating system properties")
    Settings.values().toList().forEach { setting ->
      LOG.info("Property '${setting.key}' = '${setting.getUnsecured()}'")
    }
  }
}