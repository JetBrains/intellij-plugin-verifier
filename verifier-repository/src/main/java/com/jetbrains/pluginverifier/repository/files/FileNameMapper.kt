package com.jetbrains.pluginverifier.repository.files

/**
 * Interface responsible for providing names
 * of files and directories by specified keys.
 */
interface FileNameMapper<K> {

  /**
   * Provides unique name of a file or directory by specified key.
   *
   * For instance, every plugin from the Plugin Repository has
   * unique update id, which is a good candidate for naming
   * a plugin's file ([PluginFileNameMapper])
   */
  fun getFileNameWithoutExtension(key: K): String

}