package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.dependencies.DependencyResolver

/**
 * @author Sergey Patrikeev
 */
class NotFoundDependencyResolver : DependencyResolver {
  override fun resolve(dependency: PluginDependency): DependencyResolver.Result = DependencyResolver.Result.NotFound("Plugin MissingPlugin doesn't have a build compatible with IU-145.500")
}