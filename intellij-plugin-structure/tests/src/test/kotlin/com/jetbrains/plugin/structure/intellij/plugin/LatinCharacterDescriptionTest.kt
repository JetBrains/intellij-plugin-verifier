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
  fun `non-latin character in the middle of the description`() {
    val description = "Pro版本|GitHub |qqGroup:777347929 MybatisSmartPlugin is a mybatis auxiliary plug-in Contains one-click generation of Dao, Service, and XML basic code Contains some commonly used annotations such as @Data, @Mapper, etc. to cooperate with generation Contains highlighting Dao methods and Xml methods, and can jump back and forth Contains some smart reminders More functions are in continuous development How to use: You must configure the Database Tools and SQL plugin Configure your database account password to make it work normally Select the table to be generated in Database, right-click to open, and select Mybatis Generator"
    var pluginProblem: PluginProblem? = null
    val problemRegistrar = ProblemRegistrar { pluginProblem = it }

    nonLatinCharacterVerifier.verify(description, problemRegistrar)

    assertNull(pluginProblem)
  }

  @Test
  fun `only non-latin character in the description`() {
    val description = "Немає нікого, хто любив би самий біль, хто б шукав його чи хотів би його зазнавати тільки через те, що він - біль..."
    var pluginProblem: PluginProblem? = null
    val problemRegistrar = ProblemRegistrar { pluginProblem = it }

    nonLatinCharacterVerifier.verify(description, problemRegistrar)
    assertTrue(hasShortOrNonLatinDescription(pluginProblem))
  }

  @Test
  fun `non-latin character in the description suffix`() {
    val description = "Cicero: Немає нікого, хто любив би самий біль, хто б шукав його чи хотів би його зазнавати тільки через те, що він - біль..."

    var pluginProblem: PluginProblem? = null
    val problemRegistrar = ProblemRegistrar { pluginProblem = it }

    nonLatinCharacterVerifier.verify(description, problemRegistrar)
    assertTrue(hasShortOrNonLatinDescription(pluginProblem))
  }

  private fun hasShortOrNonLatinDescription(pluginProblem: PluginProblem?) =
    pluginProblem != null && pluginProblem is ShortOrNonLatinDescription

}