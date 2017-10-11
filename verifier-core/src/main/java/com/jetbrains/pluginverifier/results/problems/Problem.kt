package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.results.presentation.DescriptionsBundle
import com.jetbrains.pluginverifier.results.presentation.Presentable
import org.jetbrains.annotations.PropertyKey
import java.text.MessageFormat

/**
 * @author Sergey Patrikeev
 */
abstract class Problem(@PropertyKey(resourceBundle = "long.descriptions") private val messageKey: String) : Presentable {

  abstract val shortDescription: String

  abstract val fullDescription: String

  final override val shortPresentation: String
    get() = shortDescription

  override val fullPresentation: String
    get() = fullDescription

  protected fun short(vararg arguments: Any): String {
    val shortTemplate = DescriptionsBundle.getShortDescriptionTemplate(messageKey)
    return MessageFormat.format(shortTemplate, *arguments.map { it.toString() }.toTypedArray())
  }

  protected fun full(vararg arguments: Any): String {
    val template = DescriptionsBundle.getFullDescriptionTemplate(messageKey)
    val effect = DescriptionsBundle.getEffect(messageKey)
    val fullMessage = MessageFormat.format(template, *arguments.map { it.toString() }.toTypedArray())
    return "$fullMessage. $effect"
  }

  final override fun toString(): String = fullDescription

  final override fun equals(other: Any?): Boolean = other is Problem && fullDescription == other.fullDescription

  final override fun hashCode(): Int = fullDescription.hashCode()

}