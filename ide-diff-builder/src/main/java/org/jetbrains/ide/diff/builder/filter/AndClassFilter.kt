package org.jetbrains.ide.diff.builder.filter

class AndClassFilter(private val classFilters: List<ClassFilter>) : ClassFilter {
  override fun shouldProcessClass(className: String) =
    classFilters.all { it.shouldProcessClass(className) }
}