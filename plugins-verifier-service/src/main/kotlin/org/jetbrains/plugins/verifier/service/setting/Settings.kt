package org.jetbrains.plugins.verifier.service.setting

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Provides handy access to the settings and system properties.
 */
enum class Settings(
    val key: String,
    private val default: (() -> String?)? = null,
    val encrypted: Boolean = false
) {
  APP_HOME_DIRECTORY("verifier.service.home.directory"),
  MAX_DISK_SPACE_MB("verifier.service.max.disk.space.mb", { (50 * 1024).toString() }),

  CLEAR_DATABASE_ON_CORRUPTION("verifier.service.clear.database.on.corruption", { "true" }),

  /**
   * Specifies how often should the verifier service poll the verification queue from the Plugin Repository.
   */
  VERIFIER_SERVICE_SCHEDULER_PERIOD_SECONDS("verifier.service.scheduler.period.seconds", { "10" }),

  ENABLE_AVAILABLE_IDE_SERVICE("verifier.service.enable.available.ide.service", { "false" }),

  /**
   * IDE build which classes are used to extract plugins' features.
   */
  FEATURE_EXTRACTOR_IDE_BUILD("verifier.service.feature.extractor.ide.build", { "IU-183.5912.21" }),

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

  fun getAsPath(): Path = Paths.get(get())

  fun getAsBoolean(): Boolean = get().toBoolean()

  fun getAsInt(): Int = get().toInt()

  fun getAsLong(): Long = get().toLong()
}