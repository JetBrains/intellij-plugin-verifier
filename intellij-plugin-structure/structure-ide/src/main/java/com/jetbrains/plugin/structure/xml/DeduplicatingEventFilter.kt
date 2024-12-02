/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.xml

import com.jetbrains.plugin.structure.xml.DeduplicatingEventFilter.EventProcessing.Seen
import com.jetbrains.plugin.structure.xml.DeduplicatingEventFilter.EventProcessing.Unseen
import javax.xml.stream.EventFilter
import javax.xml.stream.events.XMLEvent

abstract class DeduplicatingEventFilter : EventFilter {
  private var lastEvent: EventProcessing = Unseen

  override fun accept(event: XMLEvent): Boolean {
    event.onAlreadySeen { return it }

    return doAccept(event).also {
      lastEvent = Seen(event, it)
    }
  }

  protected abstract fun doAccept(event: XMLEvent): Boolean

  private sealed class EventProcessing {
    object Unseen : EventProcessing()
    data class Seen(val event: XMLEvent, val resolution: Boolean) : EventProcessing()
  }

  private inline fun XMLEvent.onAlreadySeen(seenHandler: (Boolean) -> Boolean): Boolean {
    return when(val lastEvent = this@DeduplicatingEventFilter.lastEvent) {
      is Seen -> if (lastEvent.event === this) seenHandler(lastEvent.resolution) else false
      is Unseen -> false
    }
  }

}