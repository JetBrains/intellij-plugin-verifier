package org.jetbrains.plugins.verifier.service.setting

import com.intellij.structure.domain.IdeVersion
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager

/**
 * @author Sergey Patrikeev
 */
enum class Settings(private val key: String, private val default: (() -> String?)? = null) {
  APP_HOME_DIRECTORY("verifier.service.home.dir"),
  JDK_6_HOME("verifier.service.jdk.6.dir", { JDK_8_HOME.get() }),
  JDK_7_HOME("verifier.service.jdk.7.dir", { JDK_8_HOME.get() }),
  JDK_8_HOME("verifier.service.jdk.8.dir"),
  MAX_DISK_SPACE_MB("verifier.service.max.disk.space.mb"),
  PLUGIN_REPOSITORY_URL("verifier.service.plugin.repository.url", { "http://plugins.jetbrains.com" }),
  IDE_REPOSITORY_URL("verifier.service.ide.repository.url", { "http://www.jetbrains.com" });

  fun get(): String {
    val property = System.getProperty(key)
    if (property != null) {
      return property
    }
    return default?.invoke() ?: throw IllegalStateException("The property $key should be set")
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