/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier

import jdk.jfr.Category
import jdk.jfr.Event
import jdk.jfr.Label
import jdk.jfr.Name

@Name("com.jetbrains.pluginverifier.PluginVerification")
@Label("Plugin Verification")
@Category("Plugin Verifier", "Verification")
class PluginVerificationEvent(var pluginId: String, var target: String) : Event() {
  var classCount: Int = 0
}
