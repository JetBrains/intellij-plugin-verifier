package com.jetbrains.pluginverifier.tasks

import com.intellij.structure.plugin.PluginDependency
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.PluginCoordinate
import com.jetbrains.pluginverifier.api.Progress
import com.jetbrains.pluginverifier.dependencies.*
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

    val releaseResults = checkIde(true, pluginsToCheck, progress)
    val trunkResults = checkIde(false, pluginsToCheck, progress)

    return CheckTrunkApiResult(trunkResults, releaseResults)
  }

  private fun checkIde(isRelease: Boolean,
                       pluginsToCheck: List<UpdateInfo>,
                       progress: Progress): CheckIdeResult {
    val ideDescriptor = if (isRelease) parameters.releaseIde else parameters.trunkIde
    val pluginCoordinates = pluginsToCheck.map { PluginCoordinate.ByUpdateInfo(it) }
    val excludedPlugins = IdeResourceUtil.getBrokenPluginsListedInBuild(ideDescriptor.ide) ?: emptyList()
    val dependencyResolver = if (isRelease) ReleaseResolver() else TrunkResolver()
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

  private inner class ReleaseResolver : DependencyResolver {

    private val releaseBundledResolver = BundledPluginDependencyResolver(parameters.releaseIde.ide)

    private val releaseDownloadResolver = DownloadDependencyResolver(LastCompatibleSelector(parameters.releaseIde.ideVersion))

    override fun resolve(dependency: PluginDependency): DependencyResolver.Result {
      val result = releaseBundledResolver.resolve(dependency)
      return if (result is DependencyResolver.Result.NotFound) {
        releaseDownloadResolver.resolve(dependency)
      } else {
        result
      }
    }
  }

  private inner class TrunkResolver : DependencyResolver {

    private val trunkBundledResolver = BundledPluginDependencyResolver(parameters.trunkIde.ide)

    private val releaseDownloadResolver = DownloadDependencyResolver(LastCompatibleSelector(parameters.releaseIde.ideVersion))

    private val downloadLastUpdateResolver = DownloadDependencyResolver(LastUpdateSelector())

    override fun resolve(dependency: PluginDependency): DependencyResolver.Result {
      val bundledResult = trunkBundledResolver.resolve(dependency)
      if (bundledResult !is DependencyResolver.Result.NotFound) {
        return bundledResult
      }

      if (dependency.isModule || dependency.id in parameters.jetBrainsPluginIds) {
        return downloadLastUpdateResolver.resolve(dependency)
      }

      return releaseDownloadResolver.resolve(dependency)
    }
  }


}