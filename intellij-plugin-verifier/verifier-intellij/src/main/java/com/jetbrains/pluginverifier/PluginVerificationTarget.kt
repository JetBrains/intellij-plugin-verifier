/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.jdk.JdkVersion
import com.jetbrains.pluginverifier.repository.PluginInfo

/**
 * Contains target of the performed verification: whether the plugin has been verified against an IDE or against a plugin's APIs.
 */
sealed class PluginVerificationTarget {

  data class IDE(val ideVersion: IdeVersion, val jdkVersion: JdkVersion) : PluginVerificationTarget() {
    override fun toString() = ideVersion.asString()
  }

  data class Plugin(val plugin: PluginInfo, val jdkVersion: JdkVersion) : PluginVerificationTarget() {
    override fun toString() = plugin.toString()
  }

}