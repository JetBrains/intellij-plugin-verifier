package org.jetbrains.plugins.verifier.service.startup

import com.jetbrains.pluginverifier.misc.deleteLogged
import org.jetbrains.plugins.verifier.service.ide.IdeFilesManager
import org.jetbrains.plugins.verifier.service.service.ServerInstance
import org.jetbrains.plugins.verifier.service.service.featureExtractor.FeatureService
import org.jetbrains.plugins.verifier.service.service.ide.IdeListUpdater
import org.jetbrains.plugins.verifier.service.service.verifier.VerifierService
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.storage.FileManager
import org.jetbrains.plugins.verifier.service.util.UpdateInfoCache
import org.slf4j.LoggerFactory
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener

class ServerStartupListener : ServletContextListener {

  companion object {
    private val LOG = LoggerFactory.getLogger(ServerStartupListener::class.java)
  }

  override fun contextInitialized(sce: ServletContextEvent?) {
    LOG.info("Server is ready to start")

    validateSystemProperties()

    cleanUpTempDirs()
    prepareUpdateInfoCacheForExistingIdes()

    val verifierService = VerifierService()
    val featureService = FeatureService()
    val ideListUpdater = IdeListUpdater(ServerInstance.ideRepository)

    ServerInstance.addService(verifierService)
    ServerInstance.addService(featureService)
    ServerInstance.addService(ideListUpdater)

    if (Settings.ENABLE_PLUGIN_VERIFIER_SERVICE.getAsBoolean()) {
      verifierService.start()
    }
    if (Settings.ENABLE_FEATURE_EXTRACTOR_SERVICE.getAsBoolean()) {
      featureService.start()
    }
    if (Settings.ENABLE_IDE_LIST_UPDATER.getAsBoolean()) {
      ideListUpdater.start()
    }
  }


  private fun prepareUpdateInfoCacheForExistingIdes() {
    try {
      IdeFilesManager.ideList().forEach {
        ServerInstance.pluginRepository.getLastCompatibleUpdates(it).forEach {
          UpdateInfoCache.update(it)
        }
      }
    } catch (e: Exception) {
      LOG.error("Unable to prepare update info cache", e)
    }
  }

  private fun cleanUpTempDirs() {
    FileManager.getTempDirectory().deleteLogged()
  }


  override fun contextDestroyed(sce: ServletContextEvent?) {
    ServerInstance.close()
  }

  private fun validateSystemProperties() {
    LOG.info("Validating system properties")
    Settings.values().toList().forEach { setting ->
      LOG.info("Property '${setting.key}' = '${setting.get()}'")
    }
  }

}
