package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.PluginCreator
import java.util.*

typealias DependencyCycle = List<String>

internal class DependencyChain {
    private val chain = LinkedList<String>()

    private val _dependencyCycles: MutableList<DependencyCycle> = mutableListOf()

    val cycles: List<DependencyCycle>
      get() = _dependencyCycles

    private val visitedPluginDescriptors = mutableSetOf<String>()

    fun detectCycle(configurationFile: String): Boolean {
      if (chain.contains(configurationFile)) {
        _dependencyCycles.add(chain + configurationFile)
        return true
      }
      return false
    }

    fun extend(plugin: PluginCreator): Boolean {
      val descriptor = plugin.descriptorPath
      if (visitedPluginDescriptors.contains(descriptor)) {
        return false
      }
      chain.addLast(descriptor)
      return true
    }

    fun dropLast() {
      if (chain.isNotEmpty()) {
        chain.removeLast()
      }
    }
  }