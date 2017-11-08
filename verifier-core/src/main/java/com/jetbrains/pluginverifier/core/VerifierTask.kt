package com.jetbrains.pluginverifier.core

import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.parameters.ide.IdeDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate

data class VerifierTask(val plugin: PluginCoordinate,
                        val ideDescriptor: IdeDescriptor,
                        val dependencyFinder: DependencyFinder)