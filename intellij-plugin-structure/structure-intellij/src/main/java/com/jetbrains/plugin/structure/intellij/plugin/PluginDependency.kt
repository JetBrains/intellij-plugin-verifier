package com.jetbrains.plugin.structure.intellij.plugin

interface PluginDependency {
    val id: String
    var isOptional: Boolean
    val isModule: Boolean
    fun createNewInstance(callback: PluginDependency.() -> Unit): PluginDependency
}