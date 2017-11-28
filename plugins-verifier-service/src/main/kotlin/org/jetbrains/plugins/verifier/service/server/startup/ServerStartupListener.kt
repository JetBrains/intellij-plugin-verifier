package org.jetbrains.plugins.verifier.service.server.startup

import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.ide.IdeRepository
import com.jetbrains.pluginverifier.misc.createDir
import com.jetbrains.pluginverifier.plugin.PluginDetailsProviderImpl
import com.jetbrains.pluginverifier.repository.PublicPluginRepository
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.plugins.UpdateInfoCache
import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.service.features.FeatureService
import org.jetbrains.plugins.verifier.service.service.ide.IdeListUpdater
import org.jetbrains.plugins.verifier.service.service.jdks.JdkManager
import org.jetbrains.plugins.verifier.service.service.repository.AuthorizationData
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTasksManager
import org.jetbrains.plugins.verifier.service.service.verifier.VerifierService
import org.jetbrains.plugins.verifier.service.setting.DiskUsageDistributionSetting
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.slf4j.LoggerFactory
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener

class ServerStartupListener : ServletContextListener {

  companion object {
    private val LOG = LoggerFactory.getLogger(ServerStartupListener::class.java)

    const val SERVER_CONTEXT_KEY = "plugin.verifier.service.server.context"

  }

  private val serverContext by lazy {
    createServerContext()
  }

  private fun createServerContext(): ServerContext {
    val applicationHomeDir = Settings.APP_HOME_DIRECTORY.getAsFile().createDir()
    val loadedPluginsDir = applicationHomeDir.resolve("loaded-plugins").createDir()
    val extractedPluginsDir = applicationHomeDir.resolve("extracted-plugins").createDir()
    val ideFilesDir = applicationHomeDir.resolve("ides").createDir()

    val pluginDownloadDirSpaceSetting = getPluginDownloadDirDiskSpaceSetting()

    val pluginRepositoryUrl = Settings.DOWNLOAD_PLUGINS_REPOSITORY_URL.get()
    val pluginRepository = PublicPluginRepository(pluginRepositoryUrl, loadedPluginsDir, pluginDownloadDirSpaceSetting)
    val updateInfoCache = UpdateInfoCache(pluginRepositoryUrl)

    val pluginDetailsProvider = PluginDetailsProviderImpl(extractedPluginsDir)
    val ideRepository = IdeRepository(Settings.IDE_REPOSITORY_URL.get())
    val tasksManager = ServiceTasksManager(Settings.TASK_MANAGER_CONCURRENCY.getAsInt())

    val authorizationData = AuthorizationData(
        Settings.PLUGIN_REPOSITORY_VERIFIER_USERNAME.get(),
        Settings.PLUGIN_REPOSITORY_VERIFIER_PASSWORD.get(),
        Settings.SERVICE_ADMIN_PASSWORD.get()
    )

    val jdkManager = JdkManager(Settings.JDK_8_HOME.getAsFile())

    val settings: List<Settings> = Settings.values().toList()

    val ideDownloadDirDiskSpaceSetting = getIdeDownloadDirDiskSpaceSetting()
    return ServerContext(
        applicationHomeDir,
        IdeFilesBank(ideRepository, ideFilesDir, ideDownloadDirDiskSpaceSetting, {}),
        pluginRepository,
        pluginDetailsProvider,
        tasksManager,
        authorizationData,
        jdkManager,
        updateInfoCache,
        settings
    )
  }

  private val maxDiskSpaceUsage = SpaceAmount.ofMegabytes(Settings.MAX_DISK_SPACE_MB.getAsLong().coerceAtLeast(10000))

  private fun getIdeDownloadDirDiskSpaceSetting() =
      DiskSpaceSetting(DiskUsageDistributionSetting.IDE_DOWNLOAD_DIR.getIntendedSpace(maxDiskSpaceUsage))

  private fun getPluginDownloadDirDiskSpaceSetting() =
      DiskSpaceSetting(DiskUsageDistributionSetting.PLUGIN_DOWNLOAD_DIR.getIntendedSpace(maxDiskSpaceUsage))

  override fun contextInitialized(sce: ServletContextEvent) {
    LOG.info("Server is ready to start")

    validateSystemProperties()

    prepareUpdateInfoCacheForExistingIdes()

    val verifierService = VerifierService(serverContext, Settings.VERIFIER_SERVICE_REPOSITORY_URL.get())
    val featureService = FeatureService(serverContext, Settings.FEATURE_EXTRACTOR_REPOSITORY_URL.get())
    val ideListUpdater = IdeListUpdater(serverContext)

    serverContext.addService(verifierService)
    serverContext.addService(featureService)
    serverContext.addService(ideListUpdater)

    if (Settings.ENABLE_PLUGIN_VERIFIER_SERVICE.getAsBoolean()) {
      verifierService.start()
    }
    if (Settings.ENABLE_FEATURE_EXTRACTOR_SERVICE.getAsBoolean()) {
      featureService.start()
    }
    if (Settings.ENABLE_IDE_LIST_UPDATER.getAsBoolean()) {
      ideListUpdater.start()
    }

    sce.servletContext.setAttribute(SERVER_CONTEXT_KEY, serverContext)
  }


  private fun prepareUpdateInfoCacheForExistingIdes() {
    try {
      serverContext.ideFilesBank.getAvailableIdeVersions().forEach {
        serverContext.pluginRepository.getLastCompatibleUpdates(it).forEach {
          serverContext.updateInfoCache.addUpdateInfo(it)
        }
      }
    } catch (e: Exception) {
      LOG.error("Unable to prepare update info cache", e)
    }
  }


  override fun contextDestroyed(sce: ServletContextEvent?) {
    serverContext.close()
  }

  private fun validateSystemProperties() {
    LOG.info("Validating system properties")
    Settings.values().toList().forEach { setting ->
      LOG.info("Property '${setting.key}' = '${setting.get()}'")
    }
  }
}