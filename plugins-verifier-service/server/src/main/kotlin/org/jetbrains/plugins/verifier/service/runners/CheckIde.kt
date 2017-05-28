package org.jetbrains.plugins.verifier.service.runners

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.JdkDescriptor
import com.jetbrains.pluginverifier.api.PluginCoordinate
import com.jetbrains.pluginverifier.api.ProblemsFilter
import com.jetbrains.pluginverifier.configurations.CheckIdeConfiguration
import com.jetbrains.pluginverifier.configurations.CheckIdeParams
import com.jetbrains.pluginverifier.configurations.CheckIdeParamsParser
import com.jetbrains.pluginverifier.configurations.CheckIdeResults
import com.jetbrains.pluginverifier.ide.IdeCreator
import com.jetbrains.pluginverifier.misc.deleteLogged
import org.jetbrains.plugins.verifier.service.core.BridgeVProgress
import org.jetbrains.plugins.verifier.service.core.Progress
import org.jetbrains.plugins.verifier.service.core.Task
import org.jetbrains.plugins.verifier.service.params.CheckIdeRunnerParams
import org.jetbrains.plugins.verifier.service.storage.JdkManager
import org.slf4j.LoggerFactory
import java.io.File

class CheckIdeRunner(val ideFile: File,
                     val deleteOnCompletion: Boolean,
                     val runnerParams: CheckIdeRunnerParams) : Task<CheckIdeResults>() {

  companion object {
    private val LOG = LoggerFactory.getLogger(CheckIdeRunner::class.java)
  }

  override fun presentableName(): String = "CheckIde"

  override fun computeResult(progress: Progress): CheckIdeResults {
    try {
      val ideDescriptor: IdeDescriptor = IdeCreator.createByFile(ideFile, runnerParams.actualIdeVersion)
      val pluginsToCheck = CheckIdeParamsParser().getDescriptorsToCheck(runnerParams.checkAllBuilds, runnerParams.checkLastBuilds, ideDescriptor.ideVersion)
      return doCheckIde(ideDescriptor, pluginsToCheck, progress)
    } finally {
      if (deleteOnCompletion) {
        ideFile.deleteLogged()
      }
    }
  }

  private fun doCheckIde(ideDescriptor: IdeDescriptor,
                         pluginsToCheck: List<PluginCoordinate>,
                         progress: Progress): CheckIdeResults {
    val jdkDescriptor = JdkDescriptor(JdkManager.getJdkHome(runnerParams.jdkVersion))
    val checkIdeParams = CheckIdeParams(ideDescriptor, jdkDescriptor, pluginsToCheck, runnerParams.excludedPlugins, runnerParams.pluginIdsToCheckExistingBuilds, Resolver.getEmptyResolver(), emptyList(), ProblemsFilter.AlwaysTrue, BridgeVProgress(progress))
    LOG.debug("CheckIde #$taskId arguments: $checkIdeParams")
    return CheckIdeConfiguration().execute(checkIdeParams)
  }

}