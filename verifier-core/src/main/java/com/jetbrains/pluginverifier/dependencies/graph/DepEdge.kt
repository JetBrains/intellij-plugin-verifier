package com.jetbrains.pluginverifier.dependencies.graph

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import org.jgrapht.graph.DefaultEdge

/**
 * Edge in the internal dependencies graph that records
 * the [dependency] from which this edge originates.
 */
data class DepEdge(val dependency: PluginDependency) : DefaultEdge() {
  public override fun getTarget() = super.getTarget() as DepVertex

  public override fun getSource() = super.getSource() as DepVertex
}