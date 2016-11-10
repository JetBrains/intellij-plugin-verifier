package org.jetbrains.plugins.verifier.service.setting

/**
 * @author Sergey Patrikeev
 */
enum class Settings(private val key: String,
                    private val default: (() -> String?)? = null,
                    val encrypted: Boolean = false) {
  APP_HOME_DIRECTORY("verifier.service.home.dir"),
  JDK_6_HOME("verifier.service.jdk.6.dir", { JDK_8_HOME.get() }),
  JDK_7_HOME("verifier.service.jdk.7.dir", { JDK_8_HOME.get() }),
  JDK_8_HOME("verifier.service.jdk.8.dir"),
  MAX_DISK_SPACE_MB("verifier.service.max.disk.space.mb", { (50 * 1024).toString() }),
  PLUGIN_REPOSITORY_URL("verifier.service.plugin.repository.url", { "https://plugins.jetbrains.com" }),
  IDE_REPOSITORY_URL("verifier.service.ide.repository.url", { "https://www.jetbrains.com" }),
  PLUGIN_REPOSITORY_VERIFIER_USERNAME("verifier.service.plugin.repository.verifier.username"),
  PLUGIN_REPOSITORY_VERIFIER_PASSWORD("verifier.service.plugin.repository.verifier.password", encrypted = true),
  USE_SAME_REPOSITORY_FOR_DOWNLOADING("verifier.service.use.same.repository.for.downloading", { "true" });

  fun get(): String {
    val property = System.getProperty(key)
    if (property != null) {
      return property
    }
    return default?.invoke() ?: throw IllegalStateException("The property $key should be set")
  }
}