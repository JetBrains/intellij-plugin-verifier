package com.jetbrains.pluginverifier.tests.dependencies

import com.jetbrains.pluginverifier.dependencies.resolution.CompositeDependencyFinder
import com.jetbrains.pluginverifier.tests.mocks.MockDependencyFinder
import junit.framework.TestCase.assertEquals
import org.junit.Test

class DependencyFinderTest {
  @Test
  fun `nested composite dependency finder is flattened`() {
    val alpha = MockDependencyFinder("alpha")
    val beta = MockDependencyFinder("beta")
    val alphaBetaFinder = CompositeDependencyFinder(listOf(alpha, beta))

    val gamma = MockDependencyFinder("gamma")
    val finder = CompositeDependencyFinder(listOf(alphaBetaFinder, gamma))
    val allDependencyFinders = finder.flatten()
    assertEquals(3, allDependencyFinders.size)
    assertEquals(listOf(alpha, beta, gamma), allDependencyFinders)
  }
}