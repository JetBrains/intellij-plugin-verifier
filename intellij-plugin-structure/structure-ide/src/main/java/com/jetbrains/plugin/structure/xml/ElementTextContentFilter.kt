package com.jetbrains.plugin.structure.xml

import java.util.*
import javax.xml.stream.events.Characters
import javax.xml.stream.events.EndElement
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent

class ElementTextContentFilter(private val elementXPaths: List<String>) : DeduplicatingEventFilter() {
  private val eventStack = ElementStack()

  private val captureCompleted
    get() = captureGroups.size == elementXPaths.size

  private val capturedContent = StringBuilder()

  // The ordering of keys in this map matches the ordering of values in the `elementXPaths`.
  private val captureGroups = TreeMap<String, String> { key, otherKey ->
    elementXPaths.indexOf(key).compareTo(elementXPaths.indexOf(otherKey))
  }

  override fun doAccept(event: XMLEvent): Boolean = if (captureCompleted) false else {
    when (event) {
      is StartElement -> {
        eventStack.push(event)
      }

      is EndElement -> {
        if (matchesXPath()) {
          val xPath = eventStack.toPath()
          captureGroups[xPath] = capturedContent.toString().trim()
          capturedContent.clear()
        }
        eventStack.popIf(currentEvent = event)
      }

      is Characters -> {
        if (matchesXPath()) {
          capturedContent.append(event.data)
        }
      }
    }
    true
  }

  private fun matchesXPath(): Boolean {
    val xPath = eventStack.toPath()
    return elementXPaths.any { it == xPath }
  }

  val value: String get() = captureGroups.values.first()
}