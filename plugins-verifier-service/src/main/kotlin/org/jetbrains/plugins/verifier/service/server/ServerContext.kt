package org.jetbrains.plugins.verifier.service.server

import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.ide.IdeRepository
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.parameters.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginRepository
import org.jetbrains.plugins.verifier.service.database.ServerDatabase
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.service.ide.IdeKeeper
import org.jetbrains.plugins.verifier.service.service.jdks.JdkManager
import org.jetbrains.plugins.verifier.service.setting.AuthorizationData
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.tasks.ServiceTasksManager
import java.io.Closeable
import java.nio.file.Path

/**
 * Server context aggregates all services and settings necessary to run
 * the server.
 *
 * Server context must be closed on the server shutdown to de-allocate resources.
 */
class ServerContext(val applicationHomeDirectory: Path,
                    val ideRepository: IdeRepository,
                    val ideFilesBank: IdeFilesBank,
                    val ideKeeper: IdeKeeper,
                    val pluginRepository: PluginRepository,
                    val pluginDetailsProvider: PluginDetailsProvider,
                    val taskManager: ServiceTasksManager,
                    val authorizationData: AuthorizationData,
                    val jdkManager: JdkManager,
                    val startupSettings: List<Settings>,
                    val serviceDAO: ServiceDAO,
                    val serverDatabase: ServerDatabase,
                    val ideDescriptorsCache: IdeDescriptorsCache,
                    val pluginDetailsCache: PluginDetailsCache) : Closeable {

  val allServices = arrayListOf<BaseService>()

  fun addService(service: BaseService) {
    allServices.add(service)
  }

  override fun close() {
    allServices.forEach { it.stop() }
    taskManager.closeLogged()
    serverDatabase.closeLogged()
    ideDescriptorsCache.closeLogged()
    pluginDetailsCache.closeLogged()
  }

}