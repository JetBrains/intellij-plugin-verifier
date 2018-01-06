package com.jetbrains.pluginverifier.core

import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.repository.PluginInfo

data class VerifierTask(val plugin: PluginInfo,
                        val ideDescriptor: IdeDescriptor,
                        val dependencyFinder: DependencyFinder)