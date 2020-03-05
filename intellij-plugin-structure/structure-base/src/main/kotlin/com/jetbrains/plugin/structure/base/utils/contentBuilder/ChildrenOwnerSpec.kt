package com.jetbrains.plugin.structure.base.utils.contentBuilder

interface ChildrenOwnerSpec : ContentSpec {
  fun addChild(name: String, spec: ContentSpec)
}