/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.xml

import javax.xml.stream.EventFilter
import javax.xml.stream.events.XMLEvent

typealias XmlEventType = Int

class EventTypeExcludingEventFilter(private val excludedElementTypes: Set<XmlEventType>) : EventFilter {
  constructor(vararg elementTypes: XmlEventType) : this(elementTypes.toSet())

  override fun accept(event: XMLEvent) = event.eventType !in excludedElementTypes
}