package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyResolver

/**
 * @author Sergey Patrikeev
 */
class NotFoundDependencyResolver : DependencyResolver {
  override fun findPluginDependency(dependency: PluginDependency): DependencyResolver.Result =
      DependencyResolver.Result.NotFound("Plugin ${dependency.id} doesn't have a build compatible with IU-145.500")
}