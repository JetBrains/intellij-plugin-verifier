package org.jetbrains.plugins.verifier.service.setting

import grails.util.Holders

/**
 * @author Sergey Patrikeev
 */
object Settings {

  const val APP_HOME_DIRECTORY = "verifierService.filesDirectory"
  const val JDK_6_HOME: String = "verifierService.jdk.home.6"
  const val JDK_7_HOME = "verifierService.jdk.home.7"
  const val JDK_8_HOME = "verifierService.jdk.home.8"

  fun getProperty(prop: String): String? {
    if (APP_HOME_DIRECTORY.equals(prop)) {
      return System.getProperty("user.home") + "/.pluginVerifierService" //TODO: replace
    }
    if (System.getProperty(prop) != null) {
      return System.getProperty(prop)
    }
    return Holders.getGrailsApplication().config.getProperty(prop)
  }


}

operator fun Settings.get(key: String) = Settings.getProperty(key)