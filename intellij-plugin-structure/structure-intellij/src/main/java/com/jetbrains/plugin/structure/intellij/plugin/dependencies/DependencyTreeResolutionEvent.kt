/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import jdk.jfr.Category
import jdk.jfr.Event
import jdk.jfr.Label
import jdk.jfr.Name

@Name("com.jetbrains.pluginverifier.DependencyTreeResolution")
@Label("Dependency Tree Resolution")
@Category("Plugin Verifier", "Verification")
class DependencyTreeResolutionEvent(var pluginId: String) : Event() {
  var dependencyCount: Int = 0
}
