/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.xml

import javax.xml.namespace.QName
import javax.xml.stream.EventFilter
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent

/**
 * All events that belong to the document root from the `supportedRootElements` are passed down to a delegate filter.
 * All other elements are automatically accepted.
 */
class DocumentTypeFilter(private val supportedRootElements: Set<String>, private val delegateEventFilter: EventFilter) : DeduplicatingEventFilter() {
  private var rootElement: QName? = null

  private var isPassThruMode: Boolean = false

  override fun doAccept(event: XMLEvent): Boolean {
    if (event is StartElement) {
      if (rootElement == null) {
        rootElement = event.name
        if (event.name.localPart !in supportedRootElements) {
          isPassThruMode = true
        }
      }
    }
    return if (isPassThruMode) true else delegateEventFilter.accept(event)
  }
}