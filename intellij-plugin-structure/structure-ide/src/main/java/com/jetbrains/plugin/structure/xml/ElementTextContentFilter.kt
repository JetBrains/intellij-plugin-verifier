package com.jetbrains.plugin.structure.xml

import javax.xml.stream.events.Characters
import javax.xml.stream.events.EndElement
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent

class ElementTextContentFilter(private val elementXPath: String) : DeduplicatingEventFilter() {
  private val eventStack = ElementStack()

  private var isAccepting = true

  private val capturedContent = StringBuilder()

  override fun doAccept(event: XMLEvent): Boolean = when (event) {
    is StartElement -> {
      eventStack.push(event)
      isAccepting = supports()
      isAccepting
    }

    is EndElement -> {
      isAccepting = supports()
      eventStack.popIf(currentEvent = event)
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

  private fun supports(): Boolean {
    return elementXPath == eventStack.toPath()
  }

  val value: String get() = capturedContent.toString().trim()
}