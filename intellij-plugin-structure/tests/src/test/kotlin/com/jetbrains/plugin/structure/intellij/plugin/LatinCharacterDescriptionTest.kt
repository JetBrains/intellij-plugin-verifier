package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.problems.ShortOrNonLatinDescription
import com.jetbrains.plugin.structure.intellij.verifiers.PluginDescriptionVerifier.NonLatinCharacterVerifier
import com.jetbrains.plugin.structure.intellij.verifiers.ProblemRegistrar
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LatinCharacterDescriptionTest {

  private lateinit var nonLatinCharacterVerifier: NonLatinCharacterVerifier

  @Before
  fun setUp() {
    nonLatinCharacterVerifier = NonLatinCharacterVerifier()
  }

  @Test
  fun `description with some non-latin characters is valid`() {
    val description = "Нью-Йорк| A lorem ipsum dolor sit amet adepiscit velit"
    var pluginProblem: PluginProblem? = null
    val problemRegistrar = ProblemRegistrar { pluginProblem = it }

    nonLatinCharacterVerifier.verify(description, problemRegistrar)

    assertNull(pluginProblem)
  }

  @Test
  fun `description with only non-latin characters is not valid`() {
    val description = "Немає нікого, хто любив би самий біль, хто б шукав його чи хотів би його зазнавати тільки через те, що він - біль..."
    var pluginProblem: PluginProblem? = null
    val problemRegistrar = ProblemRegistrar { pluginProblem = it }

    nonLatinCharacterVerifier.verify(description, problemRegistrar)
    assertTrue(hasShortOrNonLatinDescription(pluginProblem))
  }

  @Test
  fun `description with some latin characters in the beginning, but in not enough amount, is not valid`() {
    val description = "Cicero: Немає нікого, хто любив би самий біль, хто б шукав його чи хотів би його зазнавати тільки через те, що він - біль..."

    var pluginProblem: PluginProblem? = null
    val problemRegistrar = ProblemRegistrar { pluginProblem = it }

    nonLatinCharacterVerifier.verify(description, problemRegistrar)
    assertTrue(hasShortOrNonLatinDescription(pluginProblem))
  }

  @Test
  fun `description with enough number of latin characters in the beginning is valid`() {
    val description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam at posuere tellus. " +
      "Немає нікого, хто любив би самий біль, хто б шукав його чи хотів би його зазнавати тільки через те, що він - біль..."

    var pluginProblem: PluginProblem? = null
    val problemRegistrar = ProblemRegistrar { pluginProblem = it }

    nonLatinCharacterVerifier.verify(description, problemRegistrar)
    assertNull(pluginProblem)
  }

  private fun hasShortOrNonLatinDescription(pluginProblem: PluginProblem?) =
    pluginProblem != null && pluginProblem is ShortOrNonLatinDescription

}