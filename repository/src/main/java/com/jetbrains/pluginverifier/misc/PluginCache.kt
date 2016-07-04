package com.jetbrains.pluginverifier.misc

import com.intellij.structure.domain.Plugin
import com.intellij.structure.domain.PluginManager
import com.intellij.structure.errors.IncorrectPluginException
import java.io.File
import java.io.IOException
import java.lang.ref.SoftReference
import java.util.*

object PluginCache {

  private val myCache = HashMap<File, SoftReference<Plugin>>()

  /**
   * Returns a plugin from cache or creates it from the specified file

   * @param pluginFile file of a plugin
   * *
   * @return null if plugin is not found in the cache
   * *
   * @throws IOException if IO error occurs during attempt to create a plugin
   * *
   * @throws IncorrectPluginException if the given plugin file is incorrect
   */
  @Synchronized @Throws(IOException::class, IncorrectPluginException::class)
  fun createPlugin(pluginFile: File): Plugin {
    if (!pluginFile.exists()) {
      throw IOException("Plugin file does not exist: " + pluginFile.absoluteFile)
    }

    val softReference = myCache[pluginFile]

    var res: Plugin? = softReference?.get()

    if (res == null) {
      res = PluginManager.getInstance().createPlugin(pluginFile)
      myCache.put(pluginFile, SoftReference<Plugin>(res))
    }

    return res
  }

}
