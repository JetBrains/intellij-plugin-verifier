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
  MAX_DISK_SPACE_MB("verifier.service.max.disk.space.mb", { (50 * 1024).toString() });

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