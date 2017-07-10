package org.jetbrains.plugins.verifier.service.service.verifier

import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.plugin.Plugin
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.ide.IdeCreator
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.tasks.CheckPluginParams
import com.jetbrains.pluginverifier.tasks.CheckPluginTask
import org.jetbrains.plugins.verifier.service.ide.IdeFileLock
import org.jetbrains.plugins.verifier.service.ide.IdeFilesManager
import org.jetbrains.plugins.verifier.service.progress.BridgeVerifierProgress
import org.jetbrains.plugins.verifier.service.progress.TaskProgress
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.jetbrains.plugins.verifier.service.storage.JdkManager
import org.jetbrains.plugins.verifier.service.tasks.Task
import org.slf4j.LoggerFactory

class CheckRangeTask(val pluginInfo: PluginInfo,
                     val pluginCoordinate: PluginCoordinate,
                     val params: CheckRangeParams,
                     val ideVersions: List<IdeVersion>? = null) : Task<CheckRangeResults>() {
  override fun presentableName(): String = "Check $pluginCoordinate with IDE from [since; until]"

  companion object {
    private val LOG = LoggerFactory.getLogger(CheckRangeTask::class.java)
  }

  private fun doRangeVerification(plugin: Plugin, progress: TaskProgress): CheckRangeResults {
    val sinceBuild = plugin.sinceBuild!!
    val untilBuild = plugin.untilBuild
    val jdkDescriptor = JdkDescriptor(JdkManager.getJdkHome(params.jdkVersion))

    val ideLocks = getAvailableIdesMatchingSinceUntilBuild(sinceBuild, untilBuild)
    try {
      LOG.info("Verifying plugin $plugin [$sinceBuild; $untilBuild];\nAvailable compatible IDEs: $ideLocks;\nAll IDEs on server: ${IdeFilesManager.ideList().joinToString()};")
      if (ideLocks.isEmpty()) {
        return CheckRangeResults(pluginInfo, CheckRangeResults.ResultType.NO_COMPATIBLE_IDES, emptyList(), emptyList(), Settings.PROTOCOL_VERSION.getAsInt())
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
                                  progress: TaskProgress): CheckRangeResults {
    val ideDescriptors = arrayListOf<IdeDescriptor>()
    try {
      ideLocks.forEach { IdeCreator.createByFile(it.ideFile, null) }
      return checkPluginWithSeveralIdes(pluginCoordinate, pluginInfo, ideDescriptors, jdkDescriptor, progress)
    } finally {
      ideDescriptors.forEach { it.close() }
    }
  }

  private fun checkPluginWithSeveralIdes(pluginCoordinate: PluginCoordinate,
                                         pluginInfo: PluginInfo,
                                         ideDescriptors: List<IdeDescriptor>,
                                         jdkDescriptor: JdkDescriptor,
                                         progress: TaskProgress): CheckRangeResults {
    val verifierProgress = BridgeVerifierProgress(progress)
    val params = CheckPluginParams(listOf(pluginCoordinate), ideDescriptors, jdkDescriptor, emptyList(), ProblemsFilter.AlwaysTrue, Resolver.getEmptyResolver())
    val checkPluginTask = CheckPluginTask(params)
    val checkPluginResults = checkPluginTask.execute(verifierProgress)
    val results = checkPluginResults.results
    return CheckRangeResults(pluginInfo, CheckRangeResults.ResultType.CHECKED, ideDescriptors.map { it.ideVersion }, results, Settings.PROTOCOL_VERSION.getAsInt())
  }

  private fun getAvailableIdesMatchingSinceUntilBuild(sinceBuild: IdeVersion, untilBuild: IdeVersion?): List<IdeFileLock> = IdeFilesManager.lockAndAccess {
    (ideVersions ?: IdeFilesManager.ideList())
        .filter { sinceBuild <= it && (untilBuild == null || it <= untilBuild) }
        .mapNotNull { IdeFilesManager.getIdeLock(it) }
  }

  override fun computeResult(progress: TaskProgress): CheckRangeResults = PluginCreator.createPlugin(pluginCoordinate).use { createPluginResult ->
    val protocolVersion = Settings.PROTOCOL_VERSION.getAsInt()
    when (createPluginResult) {
      is CreatePluginResult.NotFound -> CheckRangeResults(pluginInfo, CheckRangeResults.ResultType.NOT_FOUND, emptyList(), emptyList(), protocolVersion)
      is CreatePluginResult.BadPlugin -> CheckRangeResults(pluginInfo, CheckRangeResults.ResultType.BAD_PLUGIN, emptyList(), emptyList(), protocolVersion)
      is CreatePluginResult.OK -> doRangeVerification(createPluginResult.plugin, progress)
    }
  }
}