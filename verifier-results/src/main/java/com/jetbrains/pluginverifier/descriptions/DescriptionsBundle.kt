package com.jetbrains.pluginverifier.descriptions

import com.jetbrains.pluginverifier.problems.Problem
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

  fun getFullDescription(key: String) = fullDescriptions.getProperty(key)

  fun getShortDescription(key: String) = shortDescriptions.getProperty(key)

  fun getEffect(key: String) = effects.getProperty(key)

}