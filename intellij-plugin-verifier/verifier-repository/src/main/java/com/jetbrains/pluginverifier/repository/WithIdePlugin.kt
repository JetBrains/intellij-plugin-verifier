/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin

/**
 * Indicates a [plugin info][PluginInfo] that contains a resolved [IDE Plugin][IdePlugin].
 */
interface WithIdePlugin {
  val idePlugin: IdePlugin
}