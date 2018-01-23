package com.jetbrains.pluginverifier.core

import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import com.jetbrains.pluginverifier.repository.PluginInfo

/**
 * Aggregates parameters of the upcoming verification
 * consisting of the [plugin] to be verified,
 * the [jdkPath] of the JDK to be used during the verification,
 * the [ideDescriptor] to verify the plugin against,
 * and the [dependencyFinder] that should be used to resolve
 * the dependencies of the [plugin] on other plugins and modules.
 */
data class VerifierTask(val plugin: PluginInfo,
                        val jdkPath: JdkPath,
                        val ideDescriptor: IdeDescriptor,
                        val dependencyFinder: DependencyFinder)