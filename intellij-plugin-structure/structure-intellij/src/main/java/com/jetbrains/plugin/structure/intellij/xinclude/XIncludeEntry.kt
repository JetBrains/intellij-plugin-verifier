/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.xinclude

import java.nio.file.Path

data class XIncludeEntry(val presentablePath: String, val documentPath: Path, val description: String)