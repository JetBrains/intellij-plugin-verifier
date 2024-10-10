package com.jetbrains.plugin.structure.intellij.resources

fun List<ResourceResolver>.asResolver(): ResourceResolver {
  return CompositeResourceResolver(this)
}