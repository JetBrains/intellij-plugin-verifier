package org.jetbrains.plugins.verifier.service.runners

import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeManager
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.JdkDescriptor
import com.jetbrains.pluginverifier.configurations.CheckIdeConfiguration
import com.jetbrains.pluginverifier.configurations.CheckIdeParams
import com.jetbrains.pluginverifier.configurations.CheckIdeParamsParser
import com.jetbrains.pluginverifier.configurations.CheckIdeResults
import org.jetbrains.plugins.verifier.service.core.BridgeVProgress
import org.jetbrains.plugins.verifier.service.core.Progress
import org.jetbrains.plugins.verifier.service.core.Task
import org.jetbrains.plugins.verifier.service.params.CheckIdeRunnerParams
import org.jetbrains.plugins.verifier.service.storage.JdkManager
import org.jetbrains.plugins.verifier.service.util.deleteLogged
import java.io.File

class CheckIdeRunner(val ideFile: File,
                     val deleteOnCompletion: Boolean,
                     val runnerParams: CheckIdeRunnerParams) : Task<CheckIdeResults>() {
  override fun presentableName(): String = "CheckIde"

  override fun computeImpl(progress: Progress): CheckIdeResults {
    try {
      val ide: Ide
      try {
        ide = IdeManager.getInstance().createIde(ideFile, runnerParams.actualIdeVersion)
      } catch(e: Exception) {
        throw IllegalArgumentException("The supplied IDE $ideFile is broken", e)
      }

      val pluginsToCheck = CheckIdeParamsParser.getDescriptorsToCheck(runnerParams.checkAllBuilds, runnerParams.checkLastBuilds, ide.version)

      val jdkDescriptor = JdkDescriptor.ByFile(JdkManager.getJdkHome(runnerParams.jdkVersion))
      val checkIdeParams = CheckIdeParams(IdeDescriptor.ByInstance(ide), jdkDescriptor, pluginsToCheck, runnerParams.excludedPlugins, runnerParams.vOptions, Resolver.getEmptyResolver(), BridgeVProgress(progress))

      return CheckIdeConfiguration(checkIdeParams).execute()
    } finally {
      if (deleteOnCompletion) {
        ideFile.deleteLogged()
      }
    }
  }

}