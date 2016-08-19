package org.jetbrains.plugins.verifier.service.runners

import com.intellij.structure.domain.Plugin
import com.intellij.structure.domain.PluginManager
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.configurations.CheckPluginConfiguration
import com.jetbrains.pluginverifier.configurations.CheckPluginParams
import com.jetbrains.pluginverifier.configurations.CheckRangeResults
import com.jetbrains.pluginverifier.format.UpdateInfo
import org.jetbrains.plugins.verifier.service.core.BridgeVProgress
import org.jetbrains.plugins.verifier.service.core.Progress
import org.jetbrains.plugins.verifier.service.core.Task
import org.jetbrains.plugins.verifier.service.params.CheckRangeRunnerParams
import org.jetbrains.plugins.verifier.service.storage.IdeFilesManager
import org.jetbrains.plugins.verifier.service.storage.JdkManager
import org.jetbrains.plugins.verifier.service.util.deleteLogged
import org.slf4j.LoggerFactory
import java.io.File

class CheckRangeRunner(val pluginFile: File,
                       val deleteOnCompletion: Boolean,
                       val params: CheckRangeRunnerParams,
                       val updateInfo: UpdateInfo? = null) : Task<CheckRangeResults>() {
  override fun presentableName(): String = "CheckPluginWithSinceUntilBuilds"

  companion object {
    private val LOG = LoggerFactory.getLogger(CheckRangeRunner::class.java)
  }

  override fun computeImpl(progress: Progress): CheckRangeResults {
    try {
      val plugin: Plugin
      try {
        plugin = PluginManager.getInstance().createPlugin(pluginFile)
      } catch(e: Exception) {
        LOG.error("Unable to create plugin from $pluginFile", e)
        val byFile = PluginDescriptor.ByFile("${pluginFile.name}", "", pluginFile)
        return CheckRangeResults(byFile, VResults(listOf(VResult.BadPlugin(byFile, e.message ?: e.javaClass.simpleName))))
      }

      val pluginDescriptor = PluginDescriptor.ByInstance(plugin)

      val sinceBuild = plugin.sinceBuild
      val untilBuild = plugin.untilBuild

      if (sinceBuild == null) {
        LOG.error("The plugin $pluginFile has not specified since-build property")
        return CheckRangeResults(pluginDescriptor, VResults(listOf(VResult.BadPlugin(pluginDescriptor, "The plugin ${plugin.toString()} has not specified the <idea-version> 'since-build' attribute. See  <a href=\"http://www.jetbrains.org/intellij/sdk/docs/basics/plugin_structure/plugin_configuration_file.html\">Plugin Configuration File - plugin.xml<\\a>"))))
      }

      LOG.debug("Verifying plugin $plugin against its specified [$sinceBuild; $untilBuild] builds")

      val compatibleIdes = IdeFilesManager.ideList().filter { sinceBuild.compareTo(it) <= 0 && (untilBuild == null || it.compareTo(untilBuild) <= 0) }

      LOG.debug("IDE-s on the server: ${IdeFilesManager.ideList().joinToString()}; IDE-s compatible with [$sinceBuild; $untilBuild]: [${compatibleIdes.joinToString()}]")

      if (compatibleIdes.isEmpty()) {
        //TODO: download from the IDE repository.
        LOG.error("There are no IDEs compatible with the Plugin ${plugin.toString()}; [since; until] = [$sinceBuild; $untilBuild]")
        return CheckRangeResults(pluginDescriptor, VResults(emptyList()))
      }

      val locks = compatibleIdes.map { IdeFilesManager.getIde(it) }.filterNotNull()
      try {
        val ideDescriptors = locks.map { IdeDescriptor.ByInstance(it.ide) }
        val jdkDescriptor = JdkDescriptor.ByFile(JdkManager.getJdkHome(params.jdkVersion))
        val params = CheckPluginParams(listOf(pluginDescriptor), ideDescriptors, jdkDescriptor, params.vOptions, true, Resolver.getEmptyResolver(), BridgeVProgress(progress))

        LOG.debug("CheckPlugin with [since; until] #$taskId arguments: $params")

        val results: VResults
        try {
          results = CheckPluginConfiguration(params).execute().vResults
        } catch(ie: InterruptedException) {
          throw ie
        } catch(e: Exception) {
          //this is likely the problem of the Verifier itself.
          LOG.error("Failed to verify the plugin $plugin", e)
          throw e
        }
        return CheckRangeResults(pluginDescriptor, results)
      } finally {
        locks.forEach { it.release() }
      }
    } finally {
      if (deleteOnCompletion) {
        pluginFile.deleteLogged()
      }
    }
  }
}