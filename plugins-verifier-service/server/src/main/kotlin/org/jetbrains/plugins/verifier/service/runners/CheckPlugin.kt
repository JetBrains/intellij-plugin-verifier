package org.jetbrains.plugins.verifier.service.runners

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.JdkDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.api.ProblemsFilter
import com.jetbrains.pluginverifier.configurations.CheckPluginConfiguration
import com.jetbrains.pluginverifier.configurations.CheckPluginParams
import com.jetbrains.pluginverifier.configurations.CheckPluginResults
import com.jetbrains.pluginverifier.ide.IdeCreator
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator
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
      val jdkDescriptor = JdkDescriptor(JdkManager.getJdkHome(runnerParams.jdkVersion))
      val pluginDescriptors = pluginFiles
          .map { PluginCreator.createPluginByFile(it) }
          .filterIsInstance<CreatePluginResult.OK>()
          .map { PluginDescriptor.ByInstance(it) }
      val ideDescriptors = ideFiles.map { IdeCreator.createByFile(it, null) }
      val params = CheckPluginParams(pluginDescriptors, ideDescriptors, jdkDescriptor, emptyList(), ProblemsFilter.AlwaysTrue, Resolver.getEmptyResolver(), BridgeVProgress(progress))

      LOG.debug("CheckPlugin #$taskId arguments: $params")

      val configuration = CheckPluginConfiguration()
      return configuration.execute(params)
    } finally {
      ideFiles.forEach { it.deleteLogged() }
      pluginFiles.forEach { it.deleteLogged() }
    }
  }

}