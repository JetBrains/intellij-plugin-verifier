/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
}
