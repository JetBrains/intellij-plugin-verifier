package com.jetbrains.plugin.structure.xml

import com.jetbrains.plugin.structure.xml.ElementNamesFilter.EventProcessing.Seen
import com.jetbrains.plugin.structure.xml.ElementNamesFilter.EventProcessing.Unseen
import javax.xml.namespace.QName
import javax.xml.stream.EventFilter
import javax.xml.stream.events.EndElement
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent

class ElementNamesFilter(vararg elementLocalNames: String) : EventFilter {
  private val elementLocalNames = elementLocalNames.toSet()

  private var isAccepting = true

  private val eventStack = ElementStack()

  private var lastEvent: EventProcessing = Unseen

  override fun accept(event: XMLEvent): Boolean {
    event.onAlreadySeen { return it }

    return doAccept(event).also {
      lastEvent = Seen(event, it)
    }
  }

  private fun doAccept(event: XMLEvent): Boolean = when (event) {
      is StartElement -> {
        eventStack.push(event)
        isAccepting = if (isRoot) true else supports(event.name)
        isAccepting
      }

      is EndElement -> {
        val accepts = supports(event.name)
        isAccepting = accepts
        eventStack.popIf(currentEvent = event)
        accepts
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

  private sealed class EventProcessing {
    object Unseen : EventProcessing()
    data class Seen(val event: XMLEvent, val resolution: Boolean) : EventProcessing()
  }

  private inline fun XMLEvent.onAlreadySeen(seenHandler: (Boolean) -> Boolean): Boolean {
    return when(val lastEvent = this@ElementNamesFilter.lastEvent) {
      is Seen -> if (lastEvent.event === this) seenHandler(lastEvent.resolution) else false
      is Unseen -> false
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
