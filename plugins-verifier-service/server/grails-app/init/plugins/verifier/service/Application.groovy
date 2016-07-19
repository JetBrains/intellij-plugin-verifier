package plugins.verifier.service

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.springframework.boot.actuate.health.DiskSpaceHealthIndicatorProperties

class Application extends GrailsAutoConfiguration {
  static void main(String[] args) {
    setSystemProperties()

    def context = GrailsApp.run(Application, args)

  }

  private static void setSystemProperties() {
    def appHomeDir = Settings.INSTANCE.getProperty(Settings.APP_HOME_DIRECTORY)
    System.setProperty("plugin.verifier.home.dir", appHomeDir + "/verifier")
    System.setProperty("intellij.structure.temp.dir", appHomeDir + "/intellijStructureTmp")
  }

  @Override
  Closure doWithSpring() {
    { ->
      diskSpaceHealthIndicatorProperties(DiskSpaceHealthIndicatorProperties) {
        // Set threshold to 250MB.
        threshold = 250 * 1024 * 1024
      }
    }
  }
}