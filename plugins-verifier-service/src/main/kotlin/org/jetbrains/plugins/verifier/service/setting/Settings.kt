package org.jetbrains.plugins.verifier.service.setting

import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Provides handy access to the settings and system properties.
 */
enum class Settings(val key: String,
                    private val default: (() -> String?)? = null,
                    val encrypted: Boolean = false) {
  APP_HOME_DIRECTORY("verifier.service.home.directory"),
  JDK_8_HOME("verifier.service.jdk.8.dir"),
  MAX_DISK_SPACE_MB("verifier.service.max.disk.space.mb", { (50 * 1024).toString() }),

  PLUGINS_REPOSITORY_URL("verifier.service.plugins.repository.url", { PUBLIC_PLUGIN_REPOSITORY_URL }),

  CLEAR_DATABASE_ON_CORRUPTION("verifier.service.clear.database.on.corruption", { "true" }),

  ENABLE_FEATURE_EXTRACTOR_SERVICE("verifier.service.enable.feature.extractor.service", { "false" }),
  ENABLE_PLUGIN_VERIFIER_SERVICE("verifier.service.enable.plugin.verifier.service", { "false" }),

  /**
   * Specifies how often should the verifier service poll the verification queue from the Plugin Repository.
   */
  VERIFIER_SERVICE_SCHEDULER_PERIOD_SECONDS("verifier.service.scheduler.period.seconds", { "10" }),

  ENABLE_AVAILABLE_IDE_SERVICE("verifier.service.enable.available.ide.service", { "false" }),

  /**
   * IDE build which classes are used to extract plugins' features.
   */
  FEATURE_EXTRACTOR_IDE_BUILD("verifier.service.feature.extractor.ide.build", { "IU-182.4505.22" }),

  PLUGIN_REPOSITORY_AUTHORIZATION_TOKEN("verifier.service.authorization.token", encrypted = true),

  TASK_MANAGER_CONCURRENCY("verifier.service.task.manager.concurrency", { "8" }),

  SERVICE_ADMIN_PASSWORD("verifier.service.admin.password", encrypted = true);

  fun get(): String {
    val property = System.getProperty(key)
    if (property != null) {
      return property
    }
    return default?.invoke() ?: throw IllegalStateException("The property $key should be set")
  }

  fun getUnsecured(): String {
    if (encrypted) {
      return "*****"
    }
    return get()
  }

  fun getAsURL(): URL = URL(get().trimEnd('/'))

  fun getAsPath(): Path = Paths.get(get())

  fun getAsBoolean(): Boolean = get().toBoolean()

  fun getAsInt(): Int = get().toInt()

  fun getAsLong(): Long = get().toLong()

  companion object {
    private val PUBLIC_PLUGIN_REPOSITORY_URL = "https://plugins.jetbrains.com"
  }
}