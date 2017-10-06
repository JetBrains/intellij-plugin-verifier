package com.jetbrains.pluginverifier.results.descriptions

import com.jetbrains.pluginverifier.results.problems.Problem
import java.util.*

/**
 * @author Sergey Patrikeev
 */
object DescriptionsBundle {

  private val fullDescriptions = getProperties("/long.descriptions.properties")

  private val shortDescriptions = getProperties("/short.descriptions.properties")

  private val effects = getProperties("/effects.properties")

  private fun getProperties(bundleName: String): Properties =
      Properties().apply { load(Problem::class.java.getResourceAsStream(bundleName)) }

  fun getFullDescriptionTemplate(key: String): String = fullDescriptions.getProperty(key)

  fun getShortDescriptionTemplate(key: String): String = shortDescriptions.getProperty(key)

  fun getEffect(key: String): String = effects.getProperty(key)

}