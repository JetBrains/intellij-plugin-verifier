package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder

class MockDependencyFinder : DependencyFinder {
  override val presentableName: String = "Mock Dependency Finder"

  override fun findPluginDependency(dependencyId: String, isModule: Boolean) =
    DependencyFinder.Result.NotFound("Mock Dependency Finder does not support any dependencies")
}