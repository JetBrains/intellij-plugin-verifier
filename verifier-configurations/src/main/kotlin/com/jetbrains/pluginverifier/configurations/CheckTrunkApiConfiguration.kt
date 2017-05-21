package com.jetbrains.pluginverifier.configurations

import com.google.common.collect.ImmutableMultimap
import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.plugin.Plugin
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.dependencies.DefaultDependencyResolver
import com.jetbrains.pluginverifier.dependency.DependencyResolver
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.repository.UpdateInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Sergey Patrikeev
 */
class CheckTrunkApiConfiguration : Configuration<CheckTrunkApiParams, CheckTrunkApiResults> {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(CheckTrunkApiConfiguration::class.java)
  }

  private lateinit var params: CheckTrunkApiParams
  private lateinit var trunkVersion: IdeVersion
  private lateinit var releaseVersion: IdeVersion

  private fun getCustomizedDependencyResolver() = object : DependencyResolver {
    private val trunkResolver = DefaultDependencyResolver(params.trunkDescriptor.ide)
    private val releaseResolver = DefaultDependencyResolver(params.releaseDescriptor.ide)

    override fun resolve(dependencyId: String, isModule: Boolean, dependent: Plugin): DependencyResolver.Result {
      val result = trunkResolver.resolve(dependencyId, isModule, dependent)
      return if (result is DependencyResolver.Result.NotFound) {
        releaseResolver.resolve(dependencyId, isModule, dependent)
      } else {
        result
      }
    }
  }

  private fun getUpdatesToCheck(): List<UpdateInfo> {
    val lastUpdatesCompatibleWithTrunk = RepositoryManager.getLastCompatibleUpdates(trunkVersion)
    val updatesCompatibleWithRelease = RepositoryManager.getLastCompatibleUpdates(releaseVersion)
    val trunkCompatiblePluginIds = lastUpdatesCompatibleWithTrunk.map { it.pluginId }.toSet()
    return lastUpdatesCompatibleWithTrunk + updatesCompatibleWithRelease.filterNot { it.pluginId in trunkCompatiblePluginIds }
  }

  override fun execute(parameters: CheckTrunkApiParams): CheckTrunkApiResults {
    params = parameters
    trunkVersion = params.trunkDescriptor.ideVersion
    releaseVersion = params.trunkDescriptor.ideVersion

    val updatesToCheck = getUpdatesToCheck()

    LOG.debug("The following updates will be checked with both #$trunkVersion and #$releaseVersion\n" +
        "The dependencies will be resolved against #$trunkVersion or against #$releaseVersion (if not found): " + updatesToCheck.joinToString())

    val dependencyResolver = getCustomizedDependencyResolver()

    val trunkResults = runCheckIdeConfiguration(params.trunkDescriptor, updatesToCheck, dependencyResolver)
    val releaseResults = runCheckIdeConfiguration(params.releaseDescriptor, updatesToCheck, dependencyResolver)

    return CheckTrunkApiResults(trunkResults, releaseResults)
  }


  private fun runCheckIdeConfiguration(ideDescriptor: IdeDescriptor,
                                       updatesToCheck: List<UpdateInfo>,
                                       dependencyResolver: DependencyResolver): CheckIdeResults {
    val pluginsDescriptors = updatesToCheck.map { PluginDescriptor.ByUpdateInfo(it) }
    val checkIdeParams = CheckIdeParams(ideDescriptor, params.jdkDescriptor, pluginsDescriptors, ImmutableMultimap.of(), emptyList(), Resolver.getEmptyResolver(), params.externalClassesPrefixes, params.problemsFilter, params.progress, dependencyResolver)
    return CheckIdeConfiguration().execute(checkIdeParams)
  }

}