package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder

/**
 * @author Sergey Patrikeev
 */
class NotFoundDependencyFinder : DependencyFinder {
  override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result =
      DependencyFinder.Result.NotFound("Plugin ${dependency.id} doesn't have a build compatible with IU-145.500")
}