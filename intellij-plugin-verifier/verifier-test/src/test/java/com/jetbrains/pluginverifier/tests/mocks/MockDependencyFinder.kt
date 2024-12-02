/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder

class MockDependencyFinder(override val presentableName: String = "Mock Dependency Finder") : DependencyFinder {

  override fun findPluginDependency(dependencyId: String, isModule: Boolean) =
    DependencyFinder.Result.NotFound("Mock Dependency Finder does not support any dependencies")
}