/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.xml

import javax.xml.stream.events.EndElement
import javax.xml.stream.events.StartElement

internal class ElementStack {
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