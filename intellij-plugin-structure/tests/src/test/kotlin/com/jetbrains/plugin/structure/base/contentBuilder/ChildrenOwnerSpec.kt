package com.jetbrains.plugin.structure.base.contentBuilder

interface ChildrenOwnerSpec : ContentSpec {
  fun addChild(name: String, spec: ContentSpec)
}