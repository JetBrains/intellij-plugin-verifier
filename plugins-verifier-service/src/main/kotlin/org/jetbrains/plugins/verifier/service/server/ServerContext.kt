package org.jetbrains.plugins.verifier.service.server

import com.jetbrains.pluginverifier.ide.IdeFilesManager
import com.jetbrains.pluginverifier.ide.IdeRepository
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.storage.FileManager
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.service.repository.AuthorizationData
import org.jetbrains.plugins.verifier.service.service.repository.UpdateInfoCache
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTasksManager
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.storage.JdkManager
import java.io.Closeable
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class ServerContext(val applicationHomeDirectory: File,
                    val fileManager: FileManager,
                    val ideFilesManager: IdeFilesManager,
                    val pluginRepository: PluginRepository,
                    val pluginDetailsProvider: PluginDetailsProvider,
                    val ideRepository: IdeRepository,
                    val taskManager: ServiceTasksManager,
                    val authorizationData: AuthorizationData,
                    val jdkManager: JdkManager,
                    val updateInfoCache: UpdateInfoCache,
                    val startupSettings: List<Settings>) : Closeable {

  val allServices = arrayListOf<BaseService>()

  fun addService(service: BaseService) {
    allServices.add(service)
  }

  override fun close() {
    taskManager.stop()
    allServices.forEach { it.stop() }
  }

}