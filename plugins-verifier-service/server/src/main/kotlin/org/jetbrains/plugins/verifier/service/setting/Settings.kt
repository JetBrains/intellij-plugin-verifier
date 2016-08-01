package org.jetbrains.plugins.verifier.service.setting

import com.intellij.structure.domain.IdeVersion
import grails.util.Holders

/**
 * @author Sergey Patrikeev
 */
enum class Settings(private val key: String) {
  APP_HOME_DIRECTORY("verifierService.homeDirectory"),
  JDK_6_HOME("verifierService.jdk.home.6"),
  JDK_7_HOME("verifierService.jdk.home.7"),
  JDK_8_HOME("verifierService.jdk.home.8"),
  MAX_DISK_SPACE("verifierService.max.disk.space");

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