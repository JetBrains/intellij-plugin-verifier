package org.jetbrains.plugins.verifier.service.setting

import com.intellij.structure.domain.IdeVersion
import grails.util.Holders
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager

/**
 * @author Sergey Patrikeev
 */
enum class Settings(private val key: String) {
  APP_HOME_DIRECTORY("VERIFIER_SERVICE_HOME_DIRECTORY"),
  JDK_6_HOME("VERIFIER_SERVICE_JDK_6_HOME"),
  JDK_7_HOME("VERIFIER_SERVICE_JDK_7_HOME"),
  JDK_8_HOME("VERIFIER_SERVICE_JDK_8_HOME"),
  MAX_DISK_SPACE_MB("VERIFIER_SERVICE_MAX_DISK_SPACE");

  fun get(): String {
    val property = System.getProperty(key)
    if (property != null) {
      return property
    }
    return Holders.getGrailsApplication().config.getProperty(key) ?: throw IllegalStateException("The property $key should be set")
  }
}

object TrunkVersions {

  private val versions: MutableMap<Int, IdeVersion> = hashMapOf()

  @Synchronized
  fun getReleaseVersion(trunkNumber: Int): IdeVersion? = versions[trunkNumber] ?: IdeFilesManager.ideList().filter { it.baselineVersion == trunkNumber }.sorted().firstOrNull()

  @Synchronized
  fun setReleaseVersion(trunkNumber: Int, ideVersion: IdeVersion) {
    versions[trunkNumber] = ideVersion
  }

}