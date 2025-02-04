package com.jetbrains.plugin.structure.xml

import javax.xml.stream.events.Characters
import javax.xml.stream.events.EndElement
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent

class ElementTextContentFilter(private val elementXPath: String) : DeduplicatingEventFilter() {
  private val eventStack = ElementStack()

  private var isAccepting = true

  private var captureCompleted = false

  private val capturedContent = StringBuilder()

  override fun doAccept(event: XMLEvent): Boolean = if (captureCompleted) false else {
    when (event) {
      is StartElement -> {
        eventStack.push(event)
        isAccepting = matchesXPath()
        isAccepting
      }

      is EndElement -> {
        eventStack.popIf(currentEvent = event)
        if (matchesXPath()) captureCompleted = true
        isAccepting = false
        isAccepting
      }

      is Characters -> {
        if (isAccepting) {
          capturedContent.append(event.data)
        }
        isAccepting
      }

      else -> {
        isAccepting
      }
    }
  }

  private fun matchesXPath(): Boolean = elementXPath == eventStack.toPath()

  val value: String get() = capturedContent.toString().trim()
}