package org.jetbrains.plugins.verifier.service.service

import com.google.gson.Gson
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.plugin.PluginDetailsProviderImpl
import com.jetbrains.pluginverifier.repository.IdeRepository
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.PublicPluginRepository
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.storage.FileManager
import org.jetbrains.plugins.verifier.service.storage.FileType
import org.jetbrains.plugins.verifier.service.tasks.ServiceTasksManager
import java.io.Closeable
import java.io.File

/**
 * @author Sergey Patrikeev
 */
object ServerInstance : Closeable {
  val GSON: Gson = Gson()

  private val loadedPluginsDir: File by lazy {
    File(FileManager.getAppHomeDirectory(), "loaded-plugins")
  }

  private val extractedPluginsDir: File by lazy {
    File(FileManager.getAppHomeDirectory(), "extracted-plugins")
  }

  private val MIN_DISK_SPACE_MB: Int = 10000

  //50% of available disk space is for plugins download dir
  private val DOWNLOAD_DIR_PROPORTION: Double = 0.5

  private val downloadDirMaxSpaceMb: Long by lazy {
    val diskSpace = Settings.MAX_DISK_SPACE_MB.getAsInt()
    if (diskSpace < MIN_DISK_SPACE_MB) {
      throw IllegalStateException("Too few available disk space: required at least $MIN_DISK_SPACE_MB Mb")
    } else {
      (diskSpace * DOWNLOAD_DIR_PROPORTION).toLong()
    }
  }

  val pluginRepository: PluginRepository by lazy {
    PublicPluginRepository(Settings.DOWNLOAD_PLUGINS_REPOSITORY_URL.get(), loadedPluginsDir, downloadDirMaxSpaceMb)
  }

  val pluginDetailsProvider: PluginDetailsProvider by lazy {
    PluginDetailsProviderImpl(extractedPluginsDir)
  }

  val ideRepository: IdeRepository = IdeRepository(FileManager.getTypeDir(FileType.IDE), Settings.IDE_REPOSITORY_URL.get())

  val taskManager = ServiceTasksManager(Settings.TASK_MANAGER_CONCURRENCY.getAsInt())

  val services = arrayListOf<BaseService>()

  fun addService(service: BaseService) {
    services.add(service)
  }

  override fun close() {
    taskManager.stop()
    services.forEach { it.stop() }
  }

}