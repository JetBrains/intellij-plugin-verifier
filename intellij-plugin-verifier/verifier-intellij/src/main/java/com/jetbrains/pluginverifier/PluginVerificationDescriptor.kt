/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.jdk.JdkVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.resolution.ClassResolverProvider
import com.jetbrains.pluginverifier.resolution.DefaultClassResolverProvider
import com.jetbrains.pluginverifier.resolution.PluginApiClassResolverProvider

/**
 * Describes details of upcoming plugin verification: plugin and IDE or plugin to verify against.
 */
sealed class PluginVerificationDescriptor {

  abstract val checkedPlugin: PluginInfo

  abstract val classResolverProvider: ClassResolverProvider

  abstract val presentableName: String

  /**
   * Upcoming verification of a plugin against a specific IDE version.
   * @param ideDescriptor Descriptor of IDE that is used as a compatibility target in the verification of [checkedPlugin].
   * @param checkedPlugin Descriptor of plugin that is checked against [IdeDescriptor].
   *
   */
  class IDE(
    private val ideDescriptor: IdeDescriptor,
    override val classResolverProvider: DefaultClassResolverProvider,
    override val checkedPlugin: PluginInfo
  ) : PluginVerificationDescriptor() {

    val ide: Ide
      get() = ideDescriptor.ide

    val ideVersion: IdeVersion
      get() = ideDescriptor.ideVersion

    val jdkVersion: JdkVersion
      get() = ideDescriptor.jdkDescriptor.jdkVersion

    override val presentableName get() = "$checkedPlugin against $ideVersion"
  }

  class Plugin(
    override val checkedPlugin: PluginInfo,
    val apiPlugin: PluginInfo,
    override val classResolverProvider: PluginApiClassResolverProvider,
    val jdkVersion: JdkVersion
  ) : PluginVerificationDescriptor() {

    override val presentableName get() = "$checkedPlugin against API of $apiPlugin"
  }

  final override fun toString() = presentableName

}

fun PluginVerificationDescriptor.toTarget(): PluginVerificationTarget = when (this) {
  is PluginVerificationDescriptor.IDE -> toTarget()
  is PluginVerificationDescriptor.Plugin -> toTarget()
}

fun PluginVerificationDescriptor.Plugin.toTarget(): PluginVerificationTarget.Plugin =
  PluginVerificationTarget.Plugin(apiPlugin, jdkVersion)

fun PluginVerificationDescriptor.IDE.toTarget(): PluginVerificationTarget.IDE =
  PluginVerificationTarget.IDE(ideVersion, jdkVersion)