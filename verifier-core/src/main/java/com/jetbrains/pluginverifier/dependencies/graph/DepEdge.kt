package com.jetbrains.pluginverifier.dependencies.graph

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import org.jgrapht.graph.DefaultEdge

data class DepEdge(val dependency: PluginDependency) : DefaultEdge() {
  public override fun getTarget(): DepVertex = super.getTarget() as DepVertex

  public override fun getSource(): DepVertex = super.getSource() as DepVertex
}