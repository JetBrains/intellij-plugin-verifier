package com.jetbrains.plugin.structure.xml

import javax.xml.stream.EventFilter
import javax.xml.stream.events.XMLEvent

typealias XmlEventType = Int

class EventTypeExcludingEventFilter(private val excludedElementTypes: Set<XmlEventType>) : EventFilter {
  constructor(vararg elementTypes: XmlEventType) : this(elementTypes.toSet())

  override fun accept(event: XMLEvent) = event.eventType !in excludedElementTypes
}