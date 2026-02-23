/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.zip

import jdk.jfr.Category
import jdk.jfr.Event
import jdk.jfr.Label
import jdk.jfr.Name

@Name("com.jetbrains.pluginverifier.ZipIterate")
@Label("ZIP Iterate")
@Category("Plugin Verifier", "I/O")
class ZipIterateEvent(var zipFile: String) : Event()

@Name("com.jetbrains.pluginverifier.ZipHandleEntry")
@Label("ZIP Handle Entry")
@Category("Plugin Verifier", "I/O")
class ZipHandleEntryEvent(var zipFile: String, var entryName: String) : Event()
