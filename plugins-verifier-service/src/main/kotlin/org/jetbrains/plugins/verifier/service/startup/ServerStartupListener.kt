package org.jetbrains.plugins.verifier.service.startup

import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.repository.RepositoryManager
import org.jetbrains.plugins.verifier.service.service.FeatureService
import org.jetbrains.plugins.verifier.service.service.Service
import org.jetbrains.plugins.verifier.service.service.UpdateInfoCache
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.storage.FileManager
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager
import org.jetbrains.plugins.verifier.service.util.IdeListUpdater
import org.slf4j.LoggerFactory
import java.io.File
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener

class ServerStartupListener : ServletContextListener {

  companion object {
    private val LOG = LoggerFactory.getLogger(ServerStartupListener::class.java)

    private val PUBLIC_PLUGIN_REPOSITORY: String = "https://plugins.jetbrains.com"

    private val MIN_DISK_SPACE_MB: Int = 10000

    //50% of available disk space is for plugins download dir
    private val DOWNLOAD_DIR_PROPORTION: Double = 0.5
  }

  override fun contextInitialized(sce: ServletContextEvent?) {
    LOG.info("Server is ready to start")

    assertSystemProperties()
    setSystemProperties()

    cleanUpTempDirs()
    prepareUpdateInfoCacheForExistingIdes()

    LOG.info("Server settings: ${Settings.values().filterNot { it.encrypted }.joinToString { it.key + "=" + it.get() }}")
    if (Settings.ENABLE_PLUGIN_VERIFIER_SERVICE.getAsBoolean()) {
      Service.run()
    }
    if (Settings.ENABLE_FEATURE_EXTRACTOR_SERVICE.getAsBoolean()) {
      FeatureService.run()
    }
    if (Settings.ENABLE_IDE_LIST_UPDATER.getAsBoolean()) {
      IdeListUpdater.run()
    }
  }


  fun prepareUpdateInfoCacheForExistingIdes() {
    try {
      IdeFilesManager.ideList().forEach {
        RepositoryManager.getLastCompatibleUpdates(it).forEach {
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
    LOG.info("Stopping Verifier Service")
  }

  private fun assertSystemProperties() {
    Settings.values().toList().forEach { setting ->
      try {
        setting.get()
      } catch (e: Exception) {
        throw IllegalStateException("The property ${setting.key} must be set", e)
      }
    }
  }

  private fun setSystemProperties() {
    val appHomeDir = Settings.APP_HOME_DIRECTORY.get()
    val structureTemp = File(FileManager.getTempDirectory(), "intellijStructureTmp")
    System.setProperty("plugin.verifier.home.dir", appHomeDir + "/verifier")
    System.setProperty("intellij.structure.temp.dir", structureTemp.canonicalPath)

    if ("true" == Settings.USE_SAME_REPOSITORY_FOR_DOWNLOADING.get()) {
      System.setProperty("plugin.repository.url", Settings.PLUGIN_REPOSITORY_URL.get())
    } else {
      System.setProperty("plugin.repository.url", PUBLIC_PLUGIN_REPOSITORY)
    }

    val diskSpace = try {
      Integer.parseInt(Settings.MAX_DISK_SPACE_MB.get())
    } catch (e: Exception) {
      throw IllegalStateException("Max disk space parameter must be set!", e)
    }
    if (diskSpace < MIN_DISK_SPACE_MB) {
      throw IllegalStateException("Too few available disk space: required at least $MIN_DISK_SPACE_MB Mb")
    }
    val downloadDirSpace = diskSpace * DOWNLOAD_DIR_PROPORTION
    System.setProperty("plugin.verifier.cache.dir.max.space", downloadDirSpace.toString())
  }

}
