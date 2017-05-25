package org.jetbrains.plugins.verifier.service.runners

import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.plugin.Plugin
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.configurations.CheckPluginConfiguration
import com.jetbrains.pluginverifier.configurations.CheckPluginParams
import com.jetbrains.pluginverifier.ide.IdeCreator
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator
import org.jetbrains.plugins.verifier.service.core.BridgeVProgress
import org.jetbrains.plugins.verifier.service.core.Progress
import org.jetbrains.plugins.verifier.service.core.Task
import org.jetbrains.plugins.verifier.service.params.CheckRangeRunnerParams
import org.jetbrains.plugins.verifier.service.storage.IdeFileLock
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager
import org.jetbrains.plugins.verifier.service.storage.JdkManager
import org.slf4j.LoggerFactory

class CheckRangeRunner(val pluginInfo: PluginInfo,
                       val pluginDescriptor: PluginDescriptor,
                       val params: CheckRangeRunnerParams,
                       val ideVersions: List<IdeVersion>? = null) : Task<CheckRangeResults>() {
  override fun presentableName(): String = "Check $pluginDescriptor with IDE from [since; until]"

  companion object {
    private val LOG = LoggerFactory.getLogger(CheckRangeRunner::class.java)
  }

  private fun doRangeVerification(createOk: CreatePluginResult.OK, progress: Progress): CheckRangeResults {
    val plugin = createOk.success.plugin
    val sinceBuild = plugin.sinceBuild!!
    val untilBuild = plugin.untilBuild

    LOG.debug("Verifying plugin $plugin against its specified [$sinceBuild; $untilBuild] builds")

    val ideLocks: List<IdeFileLock> = getIdesMatchingSinceUntilBuild(sinceBuild, untilBuild)
    try {
      return checkRangeResults(sinceBuild, untilBuild, ideLocks, plugin, progress)
    } finally {
      ideLocks.forEach { it.release() }
    }
  }

  private fun checkRangeResults(sinceBuild: IdeVersion, untilBuild: IdeVersion?, ideLocks: List<IdeFileLock>, plugin: Plugin, progress: Progress): CheckRangeResults {
    LOG.debug("IDE-s on the server: ${IdeFilesManager.ideList().joinToString()}; IDE-s compatible with [$sinceBuild; $untilBuild]: [${ideLocks.joinToString { it.getIdeFile().name }}}]")
    if (ideLocks.isEmpty()) {
      LOG.info("There are no IDEs compatible with the Plugin $plugin; [since; until] = [$sinceBuild; $untilBuild]")
      return CheckRangeResults(pluginInfo, CheckRangeResults.ResultType.NO_COMPATIBLE_IDES, emptyList(), emptyList())
    }

    val ideDescriptors = ideLocks.map { IdeCreator.createByFile(it.getIdeFile(), null) }
    val jdkDescriptor = JdkDescriptor(JdkManager.getJdkHome(params.jdkVersion))
    val params = CheckPluginParams(listOf(pluginDescriptor), ideDescriptors, jdkDescriptor, emptyList(), ProblemsFilter.AlwaysTrue, Resolver.getEmptyResolver(), BridgeVProgress(progress))

    LOG.debug("CheckPlugin with [since; until] #$taskId arguments: $params")

    val checkPluginResults = CheckPluginConfiguration().execute(params)
    val results: List<Result> = checkPluginResults.results
    return CheckRangeResults(pluginInfo, CheckRangeResults.ResultType.CHECKED, ideDescriptors.map { it.ideVersion }, results)
  }

  private fun getIdesMatchingSinceUntilBuild(sinceBuild: IdeVersion, untilBuild: IdeVersion?): List<IdeFileLock> = IdeFilesManager.locked {
    (ideVersions ?: IdeFilesManager.ideList())
        .filter { sinceBuild <= it && (untilBuild == null || it <= untilBuild) }
        .map { IdeFilesManager.getIde(it) }
        .filterNotNull()
  }

  override fun computeResult(progress: Progress): CheckRangeResults = PluginCreator.createPlugin(pluginDescriptor).use { createPluginResult ->
    when (createPluginResult) {
      is CreatePluginResult.NotFound -> CheckRangeResults(pluginInfo, CheckRangeResults.ResultType.NOT_FOUND, emptyList(), emptyList())
      is CreatePluginResult.BadPlugin -> CheckRangeResults(pluginInfo, CheckRangeResults.ResultType.BAD_PLUGIN, emptyList(), emptyList())
      is CreatePluginResult.OK -> doRangeVerification(createPluginResult, progress)
    }
  }
}