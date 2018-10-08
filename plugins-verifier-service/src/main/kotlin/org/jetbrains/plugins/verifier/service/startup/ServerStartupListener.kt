package org.jetbrains.plugins.verifier.service.startup

import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.ide.ReleaseIdeRepository
import com.jetbrains.pluginverifier.misc.createDir
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsProviderImpl
import com.jetbrains.pluginverifier.plugin.PluginFilesBank
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import org.jetbrains.plugins.verifier.service.database.MapDbServerDatabase
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.server.ServiceDAO
import org.jetbrains.plugins.verifier.service.service.features.DefaultFeatureServiceProtocol
import org.jetbrains.plugins.verifier.service.service.features.FeatureExtractorService
import org.jetbrains.plugins.verifier.service.service.ide.AvailableIdeService
import org.jetbrains.plugins.verifier.service.service.ide.DefaultAvailableIdeProtocol
import org.jetbrains.plugins.verifier.service.service.verifier.DefaultVerifierServiceProtocol
import org.jetbrains.plugins.verifier.service.service.verifier.VerificationResultFilter
import org.jetbrains.plugins.verifier.service.service.verifier.VerifierService
import org.jetbrains.plugins.verifier.service.setting.AuthorizationData
import org.jetbrains.plugins.verifier.service.setting.DiskUsageDistributionSetting
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.tasks.TaskManagerImpl
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.jar.Manifest
import javax.servlet.ServletContext
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener

/**
 * Startup initializer that configures the [server context] [ServerContext]
 * according to passed [settings] [Settings].
 */
class ServerStartupListener : ServletContextListener {

  companion object {
    private val LOG = LoggerFactory.getLogger(ServerStartupListener::class.java)

    const val SERVER_CONTEXT_KEY = "plugin.verifier.service.server.context"

    private const val PLUGIN_DETAILS_CACHE_SIZE = 30

    private const val IDE_DESCRIPTORS_CACHE_SIZE = 10
  }

  private lateinit var serverContext: ServerContext

  private fun createServerContext(appVersion: String?): ServerContext {
    val applicationHomeDir = Settings.APP_HOME_DIRECTORY.getAsPath().createDir()
    val loadedPluginsDir = applicationHomeDir.resolve("loaded-plugins").createDir()
    val extractedPluginsDir = applicationHomeDir.resolve("extracted-plugins").createDir()
    val ideFilesDir = applicationHomeDir.resolve("ides").createDir()

    val pluginDownloadDirSpaceSetting = getPluginDownloadDirDiskSpaceSetting()

    val pluginRepositoryUrl = Settings.PLUGINS_REPOSITORY_URL.getAsURL()
    val pluginRepository = MarketplaceRepository(pluginRepositoryUrl)
    val pluginDetailsProvider = PluginDetailsProviderImpl(extractedPluginsDir)
    val pluginFilesBank = PluginFilesBank.create(pluginRepository, loadedPluginsDir, pluginDownloadDirSpaceSetting)
    val pluginDetailsCache = PluginDetailsCache(PLUGIN_DETAILS_CACHE_SIZE, pluginFilesBank, pluginDetailsProvider)

    val ideRepository = ReleaseIdeRepository()
    val taskManager = TaskManagerImpl(Settings.TASK_MANAGER_CONCURRENCY.getAsInt())

    val authorizationData = AuthorizationData(
        Settings.SERVICE_ADMIN_PASSWORD.get(),
        Settings.PLUGIN_REPOSITORY_AUTHORIZATION_TOKEN.get()
    )

    val jdkDescriptorsCache = JdkDescriptorsCache()

    val ideDownloadDirDiskSpaceSetting = getIdeDownloadDirDiskSpaceSetting()
    val serviceDAO = openServiceDAO(applicationHomeDir)

    val ideFilesBank = IdeFilesBank(ideFilesDir, ideRepository, ideDownloadDirDiskSpaceSetting)
    val ideDescriptorsCache = IdeDescriptorsCache(IDE_DESCRIPTORS_CACHE_SIZE, ideFilesBank)

    val verificationResultsFilter = VerificationResultFilter()

    return ServerContext(
        applicationHomeDir,
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
      LOG.error("Unable to open/create database", e)
      LOG.info("Flag to clear database on corruption is " + if (Settings.CLEAR_DATABASE_ON_CORRUPTION.getAsBoolean() == true) "ON" else "OFF")
      if (Settings.CLEAR_DATABASE_ON_CORRUPTION.getAsBoolean()) {
        LOG.info("Trying to recreate database")
        databasePath.deleteLogged()
        try {
          val recreatedDAO = createServiceDAO(databasePath)
          LOG.info("Successfully recreated database")
          return recreatedDAO
        } catch (e: Exception) {
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

  override fun contextInitialized(sce: ServletContextEvent) {
    LOG.info("Server is ready to start")

    validateSystemProperties()

    val servletContext = sce.servletContext
    val appVersion = getAppVersion(servletContext)
    serverContext = createServerContext(appVersion)

    with(serverContext) {
      addVerifierService()
      addFeatureService()
      addAvailableIdeService()

      servletContext.setAttribute(SERVER_CONTEXT_KEY, serverContext)
    }
  }

  private fun ServerContext.addVerifierService() {
    val verifierServiceProtocol = DefaultVerifierServiceProtocol(authorizationData, pluginRepository)
    val jdkPath = JdkPath(Settings.JDK_8_HOME.getAsPath())
    val verifierService = VerifierService(
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
    if (Settings.ENABLE_PLUGIN_VERIFIER_SERVICE.getAsBoolean()) {
      verifierService.start()
    }
    addService(verifierService)
  }

  private fun ServerContext.addFeatureService() {
    val featureServiceProtocol = DefaultFeatureServiceProtocol(authorizationData, pluginRepository)
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

  private fun ServerContext.addAvailableIdeService() {
    val availableIdeProtocol = DefaultAvailableIdeProtocol(authorizationData, pluginRepository)
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

  private fun getAppVersion(servletContext: ServletContext): String? =
      servletContext.getResourceAsStream("/META-INF/MANIFEST.MF")?.use { inputStream ->
        val manifest = Manifest(inputStream)
        manifest.mainAttributes.getValue("Plugin-Verifier-Service-Version")
      }

  override fun contextDestroyed(sce: ServletContextEvent?) {
    serverContext.close()
  }

  private fun validateSystemProperties() {
    LOG.info("Validating system properties")
    Settings.values().toList().forEach { setting ->
      LOG.info("Property '${setting.key}' = '${setting.getUnsecured()}'")
    }
  }
}