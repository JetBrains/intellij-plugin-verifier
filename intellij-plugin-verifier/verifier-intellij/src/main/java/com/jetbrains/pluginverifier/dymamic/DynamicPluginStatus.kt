/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dymamic

/**
 * Whether the IntelliJ plugin can be enabled/disabled without IDE restart
 * See: https://plugins.jetbrains.com/docs/intellij/dynamic-plugins.html
 */
sealed class DynamicPluginStatus {

  abstract val reasonsNotToLoadUnloadWithoutRestart: Set<String>

  object MaybeDynamic : DynamicPluginStatus() {
    override val reasonsNotToLoadUnloadWithoutRestart
      get() = emptySet<String>()
  }

  data class NotDynamic(override val reasonsNotToLoadUnloadWithoutRestart: Set<String>) : DynamicPluginStatus()
}
