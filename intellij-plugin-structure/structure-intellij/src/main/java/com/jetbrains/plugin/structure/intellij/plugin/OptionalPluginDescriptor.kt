package com.jetbrains.plugin.structure.intellij.plugin

data class OptionalPluginDescriptor(
    val dependency: PluginDependency,
    val optionalPlugin: IdePlugin,
    val configurationFilePath: String
)