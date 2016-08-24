package org.jetbrains.plugins.verifier.service

import com.google.common.base.Preconditions
import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import org.jetbrains.plugins.verifier.service.service.Service
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.DiskSpaceHealthIndicatorProperties

class Application extends GrailsAutoConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(Application.class)

  private static final int MIN_DISK_SPACE_MB = 10000

  //50% of available disk space is for plugins download dir
  private static final double DOWNLOAD_DIR_PROPORTION = 0.5

  static void main(String[] args) {
    assertSystemProperties()
    setSystemProperties()

    LOG.info("Server settings: ${Settings.values().collect { it.key + "=" + it.get() }.join(", ")}")
    Service.INSTANCE.run()

    GrailsApp.run(Application, args)
  }

  private static def assertSystemProperties() {
    Settings.values().toList().forEach { setting ->
      Preconditions.checkNotNull(System.getProperty(setting.key), "$setting.key must be specified")
    }
  }

  private static void setSystemProperties() {
    String appHomeDir = Settings.APP_HOME_DIRECTORY.get()
    System.setProperty("plugin.verifier.home.dir", appHomeDir + "/verifier")
    System.setProperty("intellij.structure.temp.dir", appHomeDir + "/intellijStructureTmp")

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

  @Override
  Closure doWithSpring() {
    { ->
      diskSpaceHealthIndicatorProperties(DiskSpaceHealthIndicatorProperties) {
        // Set threshold to 1 GB.
        threshold = 1024 * 1024 * 1024
      }
    }
  }
}