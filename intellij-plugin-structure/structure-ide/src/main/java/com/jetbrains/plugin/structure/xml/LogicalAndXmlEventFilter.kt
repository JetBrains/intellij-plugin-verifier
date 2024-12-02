/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.xml

import javax.xml.stream.EventFilter
import javax.xml.stream.events.XMLEvent

class LogicalAndXmlEventFilter(private val filters: List<EventFilter>) : EventFilter {
  override fun accept(event: XMLEvent): Boolean {
    for (filter in filters) {
      if (!filter.accept(event)) {
        return false
      }
    }
    return true
  }
}