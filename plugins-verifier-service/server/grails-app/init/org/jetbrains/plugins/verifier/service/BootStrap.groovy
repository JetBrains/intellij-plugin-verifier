package org.jetbrains.plugins.verifier.service

import com.jetbrains.pluginverifier.misc.LanguageUtilsKt
import org.jetbrains.plugins.verifier.service.service.Service
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.storage.FileManager
import org.jetbrains.plugins.verifier.service.util.IdeListUpdater
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BootStrap {

  private static final Logger LOG = LoggerFactory.getLogger(BootStrap.class)

  private static final int MIN_DISK_SPACE_MB = 10000

  //50% of available disk space is for plugins download dir
  private static final double DOWNLOAD_DIR_PROPORTION = 0.5
  private static final String PUBLIC_PLUGIN_REPOSITORY = "https://plugins.jetbrains.com"


  def init = { servletContext ->
    LOG.info("Server is ready to start")

    assertSystemProperties()
    setSystemProperties()

    cleanUpTempDirs()

    LOG.info("Server settings: ${Settings.values().findAll { !it.encrypted }.collect { it.key + "=" + it.get() }.join(", ")}")
    Service.INSTANCE.run()
    IdeListUpdater.INSTANCE.run()
  }

  def destroy = {
    LOG.info("Exiting Verifier Service gracefully")
  }

  private static def cleanUpTempDirs() {
    LanguageUtilsKt.deleteLogged(FileManager.INSTANCE.tempDirectory)
  }

  private static def assertSystemProperties() {
    Settings.values().toList().forEach { setting ->
      try {
        setting.get()
      } catch (IllegalStateException e) {
        throw new IllegalStateException("The property ${setting.key} must be set", e)
      }
    }
  }

  private static void setSystemProperties() {
    String appHomeDir = Settings.APP_HOME_DIRECTORY.get()
    def structureTemp = new File(FileManager.INSTANCE.tempDirectory, "intellijStructureTmp")
    System.setProperty("plugin.verifier.home.dir", appHomeDir + "/verifier")
    System.setProperty("intellij.structure.temp.dir", structureTemp.canonicalPath)

    if ("true" == Settings.USE_SAME_REPOSITORY_FOR_DOWNLOADING.get()) {
      System.setProperty("plugin.repository.url", Settings.PLUGIN_REPOSITORY_URL.get())
    } else {
      System.setProperty("plugin.repository.url", PUBLIC_PLUGIN_REPOSITORY)
    }

    int diskSpace
    try {
      diskSpace = Integer.parseInt(Settings.MAX_DISK_SPACE_MB.get())
    } catch (NumberFormatException e) {
      throw new IllegalStateException("Max disk space parameter must be set!", e)
    }
    if (diskSpace < MIN_DISK_SPACE_MB) {
      throw new IllegalStateException("Too few available disk space: required at least $MIN_DISK_SPACE_MB Mb")
    }
    int downloadDirSpace = diskSpace * DOWNLOAD_DIR_PROPORTION
    System.setProperty("plugin.verifier.cache.dir.max.space", downloadDirSpace.toString())
  }
}
