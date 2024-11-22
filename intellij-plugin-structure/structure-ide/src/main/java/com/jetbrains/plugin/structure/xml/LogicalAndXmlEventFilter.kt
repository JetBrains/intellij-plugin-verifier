package com.jetbrains.plugin.structure.xml

import javax.xml.stream.EventFilter
import javax.xml.stream.events.XMLEvent

class LogicalAndXmlEventFilter(private val filters: List<EventFilter>) : EventFilter {
  constructor(vararg filters: EventFilter) : this(filters.toList())

  override fun accept(event: XMLEvent): Boolean {
    for (filter in filters) {
      if (!filter.accept(event)) {
        return false
      }
    }
    return true
  }
}