package org.jetbrains.plugins.verifier.service.setting

/**
 * @author Sergey Patrikeev
 */
enum class Settings(val key: String,
                    private val default: (() -> String?)? = null,
                    val encrypted: Boolean = false) {
  APP_HOME_DIRECTORY("verifier.service.home.dir"),
  JDK_6_HOME("verifier.service.jdk.6.dir", { JDK_8_HOME.get() }),
  JDK_7_HOME("verifier.service.jdk.7.dir", { JDK_8_HOME.get() }),
  JDK_8_HOME("verifier.service.jdk.8.dir"),
  PROTOCOL_VERSION("verifier.service.protocol.version", { "1" }),
  MAX_DISK_SPACE_MB("verifier.service.max.disk.space.mb", { (50 * 1024).toString() }),
  PLUGIN_REPOSITORY_URL("verifier.service.plugin.repository.url", { "https://plugins.jetbrains.com" }),
  IDE_REPOSITORY_URL("verifier.service.ide.repository.url", { "https://www.jetbrains.com" }),
  FEATURE_EXTRACTOR_REPOSITORY_URL("verifier.service.feature.extractor.plugin.repository.url", { PLUGIN_REPOSITORY_URL.get() }),
  ENABLE_FEATURE_EXTRACTOR_SERVICE("verifier.service.enable.feature.extractor.service", { "false" }),
  ENABLE_PLUGIN_VERIFIER_SERVICE("verifier.service.enable.plugin.verifier.service", { "false" }),
  PLUGIN_REPOSITORY_VERIFIER_USERNAME("verifier.service.plugin.repository.verifier.username"),
  PLUGIN_REPOSITORY_VERIFIER_PASSWORD("verifier.service.plugin.repository.verifier.password", encrypted = true),
  USE_SAME_REPOSITORY_FOR_DOWNLOADING("verifier.service.use.same.repository.for.downloading", { "true" }),
  ENABLE_IDE_LIST_UPDATER("verifier.service.enable.ide.list.updater", { "true" });

  fun get(): String {
    val property = System.getProperty(key)
    if (property != null) {
      return property
    }
    return default?.invoke() ?: throw IllegalStateException("The property $key should be set")
  }

  fun getAsBoolean(): Boolean = get().toBoolean()

  fun getAsInt(): Int = get().toInt()
}