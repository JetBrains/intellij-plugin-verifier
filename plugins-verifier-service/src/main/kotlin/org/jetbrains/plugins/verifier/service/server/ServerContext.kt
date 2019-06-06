package org.jetbrains.plugins.verifier.service.server

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.pluginverifier.ide.IdeDescriptorsCache
import com.jetbrains.pluginverifier.ide.IdeFilesBank
import com.jetbrains.pluginverifier.ide.repositories.IdeRepository
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import org.jetbrains.plugins.verifier.service.service.BaseService
import org.jetbrains.plugins.verifier.service.service.verifier.VerificationResultFilter
import org.jetbrains.plugins.verifier.service.setting.AuthorizationData
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.tasks.TaskManager
import javax.annotation.PreDestroy

/**
 * Server context aggregates all services and settings necessary to run
 * the server.
 *
 * Server context must be closed on the server shutdown to de-allocate resources.
 */
class ServerContext(
    val appVersion: String?,
    val ideRepository: IdeRepository,
    val ideFilesBank: IdeFilesBank,
    val pluginRepository: MarketplaceRepository,
    val taskManager: TaskManager,
    val authorizationData: AuthorizationData,
    val jdkDescriptorsCache: JdkDescriptorsCache,
    val startupSettings: List<Settings>,
    val serviceDAO: ServiceDAO,
    val ideDescriptorsCache: IdeDescriptorsCache,
    val pluginDetailsCache: PluginDetailsCache,
    val verificationResultsFilter: VerificationResultFilter
) {

  private val _allServices = arrayListOf<BaseService>()

  val allServices: List<BaseService>
    get() = _allServices

  fun addService(service: BaseService) {
    _allServices.add(service)
  }

  @PreDestroy
  fun close() {
    allServices.forEach { it.stop() }
    taskManager.closeLogged()
    serviceDAO.closeLogged()
    ideDescriptorsCache.closeLogged()
    pluginDetailsCache.closeLogged()
    jdkDescriptorsCache.closeLogged()
  }

}