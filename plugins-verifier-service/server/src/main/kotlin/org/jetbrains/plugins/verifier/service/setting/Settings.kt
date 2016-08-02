package org.jetbrains.plugins.verifier.service.setting

import com.intellij.structure.domain.IdeVersion
import grails.util.Holders

/**
 * @author Sergey Patrikeev
 */
enum class Settings(private val key: String) {
  APP_HOME_DIRECTORY("VERIFIER_SERVICE_HOME_DIRECTORY"),
  JDK_6_HOME("VERIFIER_SERVICE_JDK_6_HOME"),
  JDK_7_HOME("VERIFIER_SERVICE_JDK_7_HOME"),
  JDK_8_HOME("VERIFIER_SERVICE_JDK_8_HOME"),
  MAX_DISK_SPACE("VERIFIER_SERVICE_MAX_DISK_SPACE");

  fun get(): String {
    val property = System.getProperty(key)
    if (property != null) {
      return property
    }
    return Holders.getGrailsApplication().config.getProperty(key)
  }
}

object TrunkVersions {

  private val versions: MutableMap<Int, IdeVersion> = hashMapOf()

  @Synchronized
  fun getReleaseVersion(trunkNumber: Int): IdeVersion? {
    val version = versions[trunkNumber]
    if (version != null) {
      return version
    }
    val property = System.getProperty("verifierService.trunk.$trunkNumber.release.version")
    if (property != null) {
      val ideVersion = IdeVersion.createIdeVersion(property)
      versions[trunkNumber] = ideVersion
      return ideVersion
    }
    return null
  }

  @Synchronized
  fun setReleaseVersion(trunkNumber: Int, ideVersion: IdeVersion) {
    versions[trunkNumber] = ideVersion
  }

}