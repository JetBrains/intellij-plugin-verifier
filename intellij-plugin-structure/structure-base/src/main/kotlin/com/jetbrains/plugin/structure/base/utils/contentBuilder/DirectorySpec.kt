package com.jetbrains.plugin.structure.base.utils.contentBuilder

import com.jetbrains.plugin.structure.base.utils.createDir
import com.jetbrains.plugin.structure.base.utils.toSystemIndependentName
import java.io.File

class DirectorySpec : ChildrenOwnerSpec {

  private val children = hashMapOf<String, ContentSpec>()

  override fun addChild(name: String, spec: ContentSpec) {
    check(!name.toSystemIndependentName().contains('/')) { "only simple child names are allowed: $name" }
    check(name !in children) { "'$name' was already added" }
    children[name] = spec
  }

  override fun generate(target: File) {
    target.createDir()
    for ((name, spec) in children) {
      val childFile = target.resolve(name)
      spec.generate(childFile)
    }
  }
}