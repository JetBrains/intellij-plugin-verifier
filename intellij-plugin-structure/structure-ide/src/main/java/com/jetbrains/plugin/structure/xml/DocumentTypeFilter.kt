/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.xml

import com.jetbrains.plugin.structure.xml.DocumentTypeFilter.EventProcessing.Seen
import com.jetbrains.plugin.structure.xml.DocumentTypeFilter.EventProcessing.Unseen
import javax.xml.namespace.QName
import javax.xml.stream.EventFilter
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent

class DocumentTypeFilter(private val supportedRootElements: Set<String>, private val delegateEventFilter: EventFilter) : EventFilter {
  private var lastEvent: EventProcessing = Unseen

  private var rootElement: QName? = null

  private var isPassThruMode: Boolean = false

  override fun accept(event: XMLEvent): Boolean {
    event.onAlreadySeen { return it }

    return doAccept(event).also {
      lastEvent = Seen(event, it)
    }
  }

  private fun doAccept(event: XMLEvent): Boolean {
    if (event is StartElement) {
      if (rootElement == null) {
        rootElement = event.name
        if (event.name.localPart in supportedRootElements) {
          isPassThruMode = true
          return true
        } else {
          return delegateEventFilter.accept(event)
        }
      } else {
        return if (isPassThruMode) true else delegateEventFilter.accept(event)
      }
    } else {
      return if (isPassThruMode) true else delegateEventFilter.accept(event)
    }
  }

  private sealed class EventProcessing {
    object Unseen : EventProcessing()
    data class Seen(val event: XMLEvent, val resolution: Boolean) : EventProcessing()
  }

  private inline fun XMLEvent.onAlreadySeen(seenHandler: (Boolean) -> Boolean): Boolean {
    return when(val lastEvent = this@DocumentTypeFilter.lastEvent) {
      is Seen -> if (lastEvent.event === this) seenHandler(lastEvent.resolution) else false
      is Unseen -> false
    }
  }
}