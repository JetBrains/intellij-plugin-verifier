/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.plugin

import jdk.jfr.Category
import jdk.jfr.Event
import jdk.jfr.Label
import jdk.jfr.Name

@Name("com.jetbrains.pluginverifier.PluginCreation")
@Label("Plugin Creation")
@Category("Plugin Verifier", "I/O")
class PluginCreationEvent(var pluginId: String, var pluginPath: String) : Event()
