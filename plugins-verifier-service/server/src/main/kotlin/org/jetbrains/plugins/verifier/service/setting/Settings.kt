package org.jetbrains.plugins.verifier.service.setting

import com.intellij.structure.domain.IdeVersion
import grails.util.Holders
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager

/**
 * @author Sergey Patrikeev
 */
enum class Settings(private val key: String, private val default: String? = null) {
  APP_HOME_DIRECTORY("VERIFIER_SERVICE_HOME_DIRECTORY"),
  JDK_6_HOME("VERIFIER_SERVICE_JDK_6_HOME"),
  JDK_7_HOME("VERIFIER_SERVICE_JDK_7_HOME"),
  JDK_8_HOME("VERIFIER_SERVICE_JDK_8_HOME"),
  MAX_DISK_SPACE_MB("VERIFIER_SERVICE_MAX_DISK_SPACE"),
  PLUGIN_REPOSITORY_URL("VERIFIER_SERVICE_PLUGIN_REPOSITORY_URL", "http://plugins.jetbrains.com");

  fun get(): String {
    val property = System.getProperty(key)
    if (property != null) {
      return property
    }
    val config = Holders.getGrailsApplication().config.getProperty(key)
    if (config != null) {
      return config
    }
    return default ?: throw IllegalStateException("The property $key should be set")
  }
}

object TrunkVersions {

  private val versions: MutableMap<Int, IdeVersion> = hashMapOf()

  init {
    //default settings
    versions[145] = IdeVersion.createIdeVersion("IU-145.258.11")
    versions[162] = IdeVersion.createIdeVersion("IU-162.1121.10")
    versions[163] = IdeVersion.createIdeVersion("IU-163.1914")
  }

  @Synchronized
  fun listReleaseVersions(): List<Pair<Int, IdeVersion>> = versions.entries.map { it.key to it.value }

  @Synchronized
  fun getReleaseVersion(trunkNumber: Int): IdeVersion? = versions[trunkNumber] ?: IdeFilesManager.ideList().filter { it.baselineVersion == trunkNumber }.sorted().firstOrNull()

  @Synchronized
  fun setReleaseVersion(trunkNumber: Int, ideVersion: IdeVersion) {
    versions[trunkNumber] = ideVersion
  }

}