package com.jetbrains.pluginverifier.tasks

import com.intellij.structure.ide.Ide
import com.intellij.structure.plugin.PluginDependency
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginCoordinate
import com.jetbrains.pluginverifier.api.Progress
import com.jetbrains.pluginverifier.dependencies.DependencyResolver
import com.jetbrains.pluginverifier.dependencies.DownloadCompatibleDependencyResolver
import com.jetbrains.pluginverifier.dependencies.IdeCompatibleDependencyResolver
import com.jetbrains.pluginverifier.dependencies.LastCompatibleSelector
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.utils.IdeResourceUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Sergey Patrikeev
 */
class CheckTrunkApiTask(private val parameters: CheckTrunkApiParams) : Task() {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(CheckTrunkApiTask::class.java)
  }

  override fun execute(progress: Progress): CheckTrunkApiResult {
    val releaseVersion = parameters.releaseIde.ideVersion
    val trunkVersion = parameters.trunkIde.ideVersion

    val pluginsToCheck = RepositoryManager.getLastCompatibleUpdates(releaseVersion).filterNot { it.pluginId in parameters.jetBrainsPluginIds }

    LOG.debug("The following updates will be checked with both #$trunkVersion and #$releaseVersion: " + pluginsToCheck.joinToString())

    val releaseResults = checkIde(parameters.releaseIde, pluginsToCheck, progress)
    val trunkResults = checkIde(parameters.trunkIde, pluginsToCheck, progress)

    return CheckTrunkApiResult(trunkResults, releaseResults)
  }

  private fun checkIde(ideDescriptor: IdeDescriptor,
                       pluginsToCheck: List<UpdateInfo>,
                       progress: Progress): CheckIdeResult {
    val pluginCoordinates = pluginsToCheck.map { PluginCoordinate.ByUpdateInfo(it) }
    val excludedPlugins = IdeResourceUtil.getBrokenPluginsListedInBuild(ideDescriptor.ide) ?: emptyList()
    val dependencyResolver = MyDependencyResolver(ideDescriptor.ide)
    val checkIdeParams = CheckIdeParams(ideDescriptor,
        parameters.jdkDescriptor,
        pluginCoordinates,
        excludedPlugins,
        emptyList(),
        Resolver.getEmptyResolver(),
        parameters.externalClassesPrefixes,
        parameters.problemsFilter,
        dependencyResolver
    )
    return CheckIdeTask(checkIdeParams).execute(progress)
  }

  private inner class MyDependencyResolver(ide: Ide) : DependencyResolver {
    private val checkedIdeResolver = IdeCompatibleDependencyResolver(ide)

    private val releaseDownloadResolver = DownloadCompatibleDependencyResolver(LastCompatibleSelector(parameters.releaseIde.ideVersion))

    override fun resolve(dependency: PluginDependency): DependencyResolver.Result {
      if (dependency.isModule || dependency.id in parameters.jetBrainsPluginIds) {
        return checkedIdeResolver.resolve(dependency)
      }
      return releaseDownloadResolver.resolve(dependency)
    }

  }


}