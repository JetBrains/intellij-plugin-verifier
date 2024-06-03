package com.jetbrains.plugin.structure.intellij.resources

class NamedResourceResolver(val name: String, val resolver: ResourceResolver) : ResourceResolver by resolver