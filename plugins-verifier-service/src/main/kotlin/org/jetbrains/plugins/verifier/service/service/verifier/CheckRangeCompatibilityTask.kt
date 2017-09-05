package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.ide.IdeCreator
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.tasks.CheckPluginParams
import com.jetbrains.pluginverifier.tasks.CheckPluginTask
import org.jetbrains.plugins.verifier.service.ide.IdeFileLock
import org.jetbrains.plugins.verifier.service.ide.IdeFilesManager
import org.jetbrains.plugins.verifier.service.progress.VerifierToTaskBridgeProgress
import org.jetbrains.plugins.verifier.service.storage.JdkManager
import org.jetbrains.plugins.verifier.service.tasks.Task
import org.jetbrains.plugins.verifier.service.tasks.TaskProgress
import org.slf4j.LoggerFactory

class CheckRangeCompatibilityTask(val pluginInfo: PluginInfo,
                                  val pluginCoordinate: PluginCoordinate,
                                  val params: CheckRangeParams,
                                  val ideVersions: List<IdeVersion>? = null) : Task<CheckRangeCompatibilityResult>() {
  override fun presentableName(): String = "Check $pluginCoordinate with IDE from [since; until]"

  companion object {
    private val LOG = LoggerFactory.getLogger(CheckRangeCompatibilityTask::class.java)
  }

  private fun doRangeVerification(plugin: IdePlugin, progress: TaskProgress): CheckRangeCompatibilityResult {
    val sinceBuild = plugin.sinceBuild!!
    val untilBuild = plugin.untilBuild
    val jdkDescriptor = JdkDescriptor(JdkManager.getJdkHome(params.jdkVersion))

    val ideLocks = getAvailableIdesMatchingSinceUntilBuild(sinceBuild, untilBuild)
    try {
      LOG.info("Verifying plugin $plugin [$sinceBuild; $untilBuild]; with available IDEs: ${ideLocks.joinToString()}")
      if (ideLocks.isEmpty()) {
        return CheckRangeCompatibilityResult(pluginInfo, CheckRangeCompatibilityResult.ResultType.NO_COMPATIBLE_IDES)
      }
      return checkPluginWithIdes(pluginCoordinate, pluginInfo, ideLocks, jdkDescriptor, progress)
    } finally {
      ideLocks.forEach { it.close() }
    }
  }

  private fun checkPluginWithIdes(pluginCoordinate: PluginCoordinate,
                                  pluginInfo: PluginInfo,
                                  ideLocks: List<IdeFileLock>,
                                  jdkDescriptor: JdkDescriptor,
                                  progress: TaskProgress): CheckRangeCompatibilityResult {
    val ideDescriptors = arrayListOf<IdeDescriptor>()
    try {
      ideLocks.mapTo(ideDescriptors) { IdeCreator.createByFile(it.ideFile, null) }
      return checkPluginWithSeveralIdes(pluginCoordinate, pluginInfo, ideDescriptors, jdkDescriptor, progress)
    } finally {
      ideDescriptors.forEach { it.close() }
    }
  }

  private fun checkPluginWithSeveralIdes(pluginCoordinate: PluginCoordinate,
                                         pluginInfo: PluginInfo,
                                         ideDescriptors: List<IdeDescriptor>,
                                         jdkDescriptor: JdkDescriptor,
                                         progress: TaskProgress): CheckRangeCompatibilityResult {
    val verifierProgress = VerifierToTaskBridgeProgress(progress)
    val params = CheckPluginParams(listOf(pluginCoordinate), ideDescriptors, jdkDescriptor, emptyList(), ProblemsFilter.AlwaysTrue, Resolver.getEmptyResolver())
    val checkPluginTask = CheckPluginTask(params)
    val checkPluginResults = checkPluginTask.execute(verifierProgress)
    val results = checkPluginResults.results
    return CheckRangeCompatibilityResult(pluginInfo, CheckRangeCompatibilityResult.ResultType.VERIFICATION_DONE, results)
  }

  private fun getAvailableIdesMatchingSinceUntilBuild(sinceBuild: IdeVersion, untilBuild: IdeVersion?): List<IdeFileLock> = IdeFilesManager.lockAndAccess {
    (ideVersions ?: IdeFilesManager.ideList())
        .filter { sinceBuild <= it && (untilBuild == null || it <= untilBuild) }
        .mapNotNull { IdeFilesManager.getIdeLock(it) }
  }

  override fun computeResult(progress: TaskProgress): CheckRangeCompatibilityResult = PluginCreator.createPlugin(pluginCoordinate).use { createPluginResult ->
    when (createPluginResult) {
      is CreatePluginResult.NotFound -> CheckRangeCompatibilityResult(pluginInfo, CheckRangeCompatibilityResult.ResultType.NON_DOWNLOADABLE, nonDownloadableReason = createPluginResult.reason)
      is CreatePluginResult.BadPlugin -> CheckRangeCompatibilityResult(pluginInfo, CheckRangeCompatibilityResult.ResultType.INVALID_PLUGIN, invalidPluginProblems = createPluginResult.pluginErrorsAndWarnings)
      is CreatePluginResult.OK -> doRangeVerification(createPluginResult.plugin, progress)
    }
  }
}