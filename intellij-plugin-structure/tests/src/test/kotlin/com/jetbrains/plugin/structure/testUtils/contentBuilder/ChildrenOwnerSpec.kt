package com.jetbrains.plugin.structure.testUtils.contentBuilder

interface ChildrenOwnerSpec : ContentSpec {
  fun addChild(name: String, spec: ContentSpec)
}