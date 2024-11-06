package com.jetbrains.plugin.structure.xml

import javax.xml.stream.EventFilter
import javax.xml.stream.events.XMLEvent

object PassThruFilter : EventFilter {
  override fun accept(event: XMLEvent) = true
}