package org.jetbrains.ide.diff.builder.filter

interface ClassFilter {
  fun shouldProcessClass(className: String): Boolean
}

