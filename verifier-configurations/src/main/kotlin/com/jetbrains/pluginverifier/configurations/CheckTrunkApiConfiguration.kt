package com.jetbrains.pluginverifier.configurations

import com.google.common.collect.ImmutableMultimap
import com.intellij.structure.ide.Ide
import com.intellij.structure.plugin.Plugin
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.dependencies.DefaultDependencyResolver
import com.jetbrains.pluginverifier.dependency.DependencyResolver
import com.jetbrains.pluginverifier.repository.RepositoryManager
import com.jetbrains.pluginverifier.repository.UpdateInfo

/**
 * @author Sergey Patrikeev
 */
class CheckTrunkApiConfiguration : Configuration<CheckTrunkApiParams, CheckTrunkApiResults> {

  private data class ResolveArguments(val dependencyId: String, val isModule: Boolean, val dependentId: String, val dependentVersion: String?)

  private var rememberedDependencyUpdates: MutableMap<ResolveArguments, UpdateInfo> = hashMapOf()

  private lateinit var params: CheckTrunkApiParams

  private fun getDependencyResolverForTrunkIde() = object : DependencyResolver {
    private val defaultResolver = DefaultDependencyResolver(params.trunkDescriptor.ide)

    override fun resolve(dependencyId: String, isModule: Boolean, dependent: Plugin): DependencyResolver.Result {
      val result = defaultResolver.resolve(dependencyId, isModule, dependent)
      val arguments = ResolveArguments(dependencyId, isModule, dependent.pluginId, dependent.pluginVersion)
      if (result is DependencyResolver.Result.Downloaded) {
        rememberedDependencyUpdates[arguments] = result.updateInfo
      }
      return result
    }
  }

  private fun getDependencyResolverForReleaseIde() = object : DependencyResolver {
    private val defaultResolver = DefaultDependencyResolver(params.releaseDescriptor.ide)

    override fun resolve(dependencyId: String, isModule: Boolean, dependent: Plugin): DependencyResolver.Result {
      val updateInfo = rememberedDependencyUpdates[ResolveArguments(dependencyId, isModule, dependent.pluginId, dependent.pluginVersion)]
          ?: return defaultResolver.resolve(dependencyId, isModule, dependent)

      val pluginLock = RepositoryManager.getPluginFile(updateInfo) ?: return defaultResolver.resolve(dependencyId, isModule, dependent)
      return defaultResolver.getDependencyResultByDownloadedUpdate(pluginLock, updateInfo)
    }
  }

  private fun getPluginsToCheck(): List<UpdateInfo> {
    val lastUpdatesCompatibleWithTrunk = RepositoryManager.getLastCompatibleUpdates(params.trunkDescriptor.ideVersion)
    val updatesCompatibleWithRelease = RepositoryManager.getLastCompatibleUpdates(params.releaseDescriptor.ideVersion)
    val trunkCompatiblePluginIds = lastUpdatesCompatibleWithTrunk.map { it.pluginId }.toSet()
    return lastUpdatesCompatibleWithTrunk + updatesCompatibleWithRelease.filterNot { it.pluginId in trunkCompatiblePluginIds }
  }

  override fun execute(parameters: CheckTrunkApiParams): CheckTrunkApiResults {
    params = parameters
    val pluginsToCheck = getPluginsToCheck().map { PluginDescriptor.ByUpdateInfo(it) }
    val trunkResults = runCheckIdeConfiguration(params.trunkDescriptor, pluginsToCheck, getDependencyResolverForTrunkIde())
    val trunkBundled = getBundledPlugins(params.trunkDescriptor.ide)

    val releaseResults = runCheckIdeConfiguration(params.releaseDescriptor, pluginsToCheck, getDependencyResolverForReleaseIde())
    val releaseBundled = getBundledPlugins(params.releaseDescriptor.ide)

    return CheckTrunkApiResults(trunkResults, trunkBundled, releaseResults, releaseBundled)
  }

  private fun getBundledPlugins(ide: Ide): BundledPlugins =
      BundledPlugins(ide.bundledPlugins.map { it.pluginId }.distinct(), ide.bundledPlugins.flatMap { it.definedModules }.distinct())


  private fun runCheckIdeConfiguration(ideDescriptor: IdeDescriptor,
                                       pluginsToCheck: List<PluginDescriptor>,
                                       dependencyResolver: DependencyResolver?): CheckIdeResults {
    val checkIdeParams = CheckIdeParams(ideDescriptor, params.jdkDescriptor, pluginsToCheck, ImmutableMultimap.of(), emptyList(), Resolver.getEmptyResolver(), params.externalClassesPrefixes, params.problemsFilter, params.progress, dependencyResolver)
    return CheckIdeConfiguration().execute(checkIdeParams)
  }

}