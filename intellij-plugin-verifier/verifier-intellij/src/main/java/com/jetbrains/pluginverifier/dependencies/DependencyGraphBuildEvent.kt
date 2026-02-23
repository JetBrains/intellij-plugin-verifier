/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies

import jdk.jfr.Category
import jdk.jfr.Event
import jdk.jfr.Label
import jdk.jfr.Name

@Name("com.jetbrains.pluginverifier.DependencyGraphBuild")
@Label("Dependency Graph Build")
@Category("Plugin Verifier", "Verification")
class DependencyGraphBuildEvent(var pluginId: String, var ideVersion: String) : Event() {
  var vertexCount: Int = 0
  var edgeCount: Int = 0
}
