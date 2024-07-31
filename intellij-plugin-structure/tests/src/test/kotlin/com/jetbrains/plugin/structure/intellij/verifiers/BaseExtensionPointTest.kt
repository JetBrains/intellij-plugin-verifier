package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.mocks.SimpleProblemRegistrar
import org.junit.Before

internal const val PLUGIN_ID = "com.example.thirdparty"
internal const val PLUGIN_VENDOR = "PluginIndustries s.r.o."
internal const val JETBRAINS_PLUGIN_VENDOR = "JetBrains"

abstract class BaseExtensionPointTest<V : Any>(val verifier: V) {

  protected val problemRegistrar = SimpleProblemRegistrar()

  protected val problems: List<PluginProblem>
    get() = problemRegistrar.problems

  @Before
  fun setUp() {
    problemRegistrar.reset()
  }
}