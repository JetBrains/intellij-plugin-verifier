/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.decompress

import jdk.jfr.Category
import jdk.jfr.Event
import jdk.jfr.Label
import jdk.jfr.Name

@Name("com.jetbrains.pluginverifier.Decompression")
@Label("Decompression")
@Category("Plugin Verifier", "I/O")
class DecompressionEvent(var sourceFile: String, var archiveType: String) : Event()
