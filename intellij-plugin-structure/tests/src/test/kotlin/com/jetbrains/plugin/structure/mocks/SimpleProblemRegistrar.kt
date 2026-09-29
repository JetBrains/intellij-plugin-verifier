package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.verifiers.ProblemRegistrar

class SimpleProblemRegistrar : ProblemRegistrar, List<PluginProblem> {
  private val _problems = mutableListOf<PluginProblem>()

  val problems: List<PluginProblem>
    get() = _problems

  override fun registerProblem(problem: PluginProblem) {
    _problems += problem
  }

  fun reset() {
    _problems.clear()
  }

  override val size: Int get() = _problems.size

  override fun contains(element: PluginProblem): Boolean = _problems.contains(element)

  override fun containsAll(elements: Collection<PluginProblem>): Boolean = _problems.containsAll(elements)

  override fun get(index: Int): PluginProblem = _problems[index]

  override fun indexOf(element: PluginProblem): Int = _problems.indexOf(element)

  override fun isEmpty(): Boolean = _problems.isEmpty()

  override fun iterator(): Iterator<PluginProblem> = _problems.iterator()

  override fun lastIndexOf(element: PluginProblem): Int = _problems.lastIndexOf(element)

  override fun listIterator(): ListIterator<PluginProblem> = _problems.listIterator()

  override fun listIterator(index: Int): ListIterator<PluginProblem> = _problems.listIterator(index)

  override fun subList(fromIndex: Int, toIndex: Int): List<PluginProblem> = _problems.subList(fromIndex, toIndex)
}