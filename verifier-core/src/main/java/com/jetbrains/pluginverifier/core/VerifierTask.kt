package com.jetbrains.pluginverifier.core

import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.repository.PluginInfo

/**
 * Aggregates parameters of the upcoming verification
 * that consist of the [plugin] to be verified,
 * the [ideDescriptor] to verify the plugin against,
 * and the [dependencyFinder] that should be used to resolve
 * the dependencies of the [plugin] on other plugins and modules.
 */
data class VerifierTask(val plugin: PluginInfo,
                        val ideDescriptor: IdeDescriptor,
                        val dependencyFinder: DependencyFinder)