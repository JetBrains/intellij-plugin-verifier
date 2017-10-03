package com.jetbrains.pluginverifier.tasks

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginCoordinate
import com.jetbrains.pluginverifier.api.Progress
import com.jetbrains.pluginverifier.dependencies.*
import com.jetbrains.pluginverifier.plugin.PluginCreator
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.utils.IdeResourceUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Sergey Patrikeev
 */
class CheckTrunkApiTask(private val parameters: CheckTrunkApiParams,
                        private val pluginRepository: PluginRepository,
                        private val pluginCreator: PluginCreator) : Task() {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(CheckTrunkApiTask::class.java)
  }

  override fun execute(progress: Progress): CheckTrunkApiResult {
    val releaseVersion = parameters.releaseIde.ideVersion
    val trunkVersion = parameters.trunkIde.ideVersion

    val pluginsToCheck = pluginRepository.getLastCompatibleUpdates(releaseVersion).filterNot { it.pluginId in parameters.jetBrainsPluginIds }

    LOG.debug("The following updates will be checked with both #$trunkVersion and #$releaseVersion: " + pluginsToCheck.joinToString())

    val releaseResults = checkIde(parameters.releaseIde, pluginsToCheck, ReleaseResolver(), progress)
    val trunkResults = checkIde(parameters.trunkIde, pluginsToCheck, TrunkResolver(), progress)

    return CheckTrunkApiResult(trunkResults, releaseResults)
  }

  private fun checkIde(ideDescriptor: IdeDescriptor,
                       pluginsToCheck: List<UpdateInfo>,
                       dependencyResolver: DependencyResolver,
                       progress: Progress): CheckIdeResult {
    val pluginCoordinates = pluginsToCheck.map { PluginCoordinate.ByUpdateInfo(it) }
    val excludedPlugins = IdeResourceUtil.getBrokenPluginsListedInBuild(ideDescriptor.ide) ?: emptyList()
    val checkIdeParams = CheckIdeParams(ideDescriptor,
        parameters.jdkDescriptor,
        pluginCoordinates,
        excludedPlugins,
        emptyList(),
        EmptyResolver,
        parameters.externalClassesPrefixes,
        parameters.problemsFilters,
        dependencyResolver
    )
    return CheckIdeTask(checkIdeParams, pluginRepository, pluginCreator).execute(progress)
  }

  private val downloadReleaseCompatibleResolver = DownloadDependencyResolver(LastCompatibleSelector(parameters.releaseIde.ideVersion), pluginRepository, pluginCreator)

  private inner class ReleaseResolver : DependencyResolver {

    private val releaseBundledResolver = BundledPluginDependencyResolver(parameters.releaseIde.ide, pluginCreator)

    override fun resolve(dependency: PluginDependency): DependencyResolver.Result {
      val result = releaseBundledResolver.resolve(dependency)
      return if (result is DependencyResolver.Result.NotFound) {
        downloadReleaseCompatibleResolver.resolve(dependency)
      } else {
        result
      }
    }
  }

  private inner class TrunkResolver : DependencyResolver {

    private val trunkBundledResolver = BundledPluginDependencyResolver(parameters.trunkIde.ide, pluginCreator)

    private val downloadLastUpdateResolver = DownloadDependencyResolver(LastUpdateSelector(), pluginRepository, pluginCreator)

    override fun resolve(dependency: PluginDependency): DependencyResolver.Result {
      val bundledResult = trunkBundledResolver.resolve(dependency)
      if (bundledResult !is DependencyResolver.Result.NotFound) {
        return bundledResult
      }

      if (dependency.isModule || dependency.id in parameters.jetBrainsPluginIds) {
        return downloadLastUpdateResolver.resolve(dependency)
      }

      return downloadReleaseCompatibleResolver.resolve(dependency)
    }
  }


}