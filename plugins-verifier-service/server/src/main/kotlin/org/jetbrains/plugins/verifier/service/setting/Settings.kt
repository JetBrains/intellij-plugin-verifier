package org.jetbrains.plugins.verifier.service.setting

/**
 * @author Sergey Patrikeev
 */
enum class Settings(private val key: String, private val default: (() -> String?)? = null) {
  APP_HOME_DIRECTORY("verifier.service.home.dir"),
  JDK_6_HOME("verifier.service.jdk.6.dir", { JDK_8_HOME.get() }),
  JDK_7_HOME("verifier.service.jdk.7.dir", { JDK_8_HOME.get() }),
  JDK_8_HOME("verifier.service.jdk.8.dir"),
  MAX_DISK_SPACE_MB("verifier.service.max.disk.space.mb", { (50 * 1024).toString() }),
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