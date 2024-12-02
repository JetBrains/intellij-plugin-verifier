/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.xml

import javax.xml.namespace.QName
import javax.xml.stream.events.EndElement
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent

class ElementNamesFilter(private val elementLocalNames: List<String>) : DeduplicatingEventFilter() {

  private var isAccepting = true

  private val eventStack = ElementStack()

  override fun doAccept(event: XMLEvent): Boolean = when (event) {
      is StartElement -> {
        eventStack.push(event)
        isAccepting = if (isRoot) true else supports(event.name)
        isAccepting
      }

      is EndElement -> {
        isAccepting = supports(event.name)
        eventStack.popIf(currentEvent = event)
        isAccepting
      }

      else -> {
        isAccepting
      }
    }

  private fun supports(elementName: QName): Boolean {
    return elementLocalNames.any { elementPath ->
      if (elementPath.contains("/")) {
        // stack contains the elementName on top
        elementPath == eventStack.toPath()
      } else {
        elementName.localPart == elementPath
      }
    }
  }

  private val isRoot: Boolean get() = eventStack.size == 1

  private class ElementStack {
    private val stack = ArrayDeque<StartElement>()

    val size: Int get() = stack.size

    fun push(event: StartElement) {
      if (isEmpty() || peek() !== event) {
        // no need to push the same event twice
        stack.addLast(event)
      }
    }

    fun popIf(currentEvent: EndElement) {
      if (isEmpty()) return
      val peek = stack.last()
      if (peek.name == currentEvent.name) {
        stack.removeLast()
      }
    }

    fun isEmpty() = stack.isEmpty()

    @Throws(NoSuchElementException::class)
    fun peek(): StartElement = stack.last()

    fun toPath() = stack.joinToString(prefix = "/", separator = "/") { it.name.localPart }
  }
}
