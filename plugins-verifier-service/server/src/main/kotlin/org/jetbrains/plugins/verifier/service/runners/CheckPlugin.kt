package org.jetbrains.plugins.verifier.service.runners

import com.intellij.structure.domain.IdeManager
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.JdkDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.configurations.CheckPluginConfiguration
import com.jetbrains.pluginverifier.configurations.CheckPluginParams
import com.jetbrains.pluginverifier.configurations.CheckPluginResults
import com.jetbrains.pluginverifier.misc.deleteLogged
import org.jetbrains.plugins.verifier.service.core.BridgeVProgress
import org.jetbrains.plugins.verifier.service.core.Progress
import org.jetbrains.plugins.verifier.service.core.Task
import org.jetbrains.plugins.verifier.service.params.CheckPluginRunnerParams
import org.jetbrains.plugins.verifier.service.storage.JdkManager
import org.slf4j.LoggerFactory
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class CheckPlugin(val runnerParams: CheckPluginRunnerParams,
                  val ideFiles: List<File>,
                  val pluginFiles: List<File>) : Task<CheckPluginResults>() {

  companion object {
    private val LOG = LoggerFactory.getLogger(CheckPlugin::class.java)
  }

  override fun presentableName(): String = "CheckPlugin"

  override fun computeResult(progress: Progress): CheckPluginResults {
    try {
      val jdkDescriptor = JdkDescriptor.ByFile(JdkManager.getJdkHome(runnerParams.jdkVersion))
      val pluginDescriptors = pluginFiles.map { PluginDescriptor.ByFile(it.nameWithoutExtension, "", it) }
      val ideDescriptors = ideFiles.map { IdeManager.getInstance().createIde(it) }.map { IdeDescriptor.ByInstance(it) }
      val params = CheckPluginParams(pluginDescriptors, ideDescriptors, jdkDescriptor, runnerParams.vOptions, true, Resolver.getEmptyResolver(), BridgeVProgress(progress))

      LOG.debug("CheckPlugin #$taskId arguments: $params")

      val configuration = CheckPluginConfiguration(params)
      return configuration.execute()
    } finally {
      ideFiles.forEach { it.deleteLogged() }
      pluginFiles.forEach { it.deleteLogged() }
    }
  }

}