/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.warnings

import com.jetbrains.pluginverifier.dependencies.DependencyNode

data class DependenciesCycleWarning(val cycle: List<DependencyNode>) : CompatibilityWarning() {

  override val problemType: String
    get() = "Dependencies cycle warning"

  override val shortDescription
    get() = "Plugin dependencies are cyclic"

  override val fullDescription
    get() = "The plugin is on a dependencies cycle: " + cycle.joinToString(separator = " -> ") + " -> " + cycle[0]
}