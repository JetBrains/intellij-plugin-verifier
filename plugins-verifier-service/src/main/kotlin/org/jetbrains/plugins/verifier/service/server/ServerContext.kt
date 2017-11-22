package org.jetbrains.plugins.verifier.service.server

import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginRepository
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.service.jdks.JdkManager
import org.jetbrains.plugins.verifier.service.service.repository.AuthorizationData
import org.jetbrains.plugins.verifier.service.service.repository.UpdateInfoCache
import org.jetbrains.plugins.verifier.service.service.tasks.ServiceTasksManager
import org.jetbrains.plugins.verifier.service.setting.Settings
import java.io.Closeable
import java.nio.file.Path

/**
 * @author Sergey Patrikeev
 */
class ServerContext(val applicationHomeDirectory: Path,
                    val ideFilesBank: IdeFilesBank,
                    val pluginRepository: PluginRepository,
                    val pluginDetailsProvider: PluginDetailsProvider,
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