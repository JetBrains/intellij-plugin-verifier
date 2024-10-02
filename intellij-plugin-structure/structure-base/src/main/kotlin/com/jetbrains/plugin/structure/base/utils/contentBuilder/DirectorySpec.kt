/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils.contentBuilder

import com.jetbrains.plugin.structure.base.utils.createDir
import com.jetbrains.plugin.structure.base.utils.toSystemIndependentName
import java.nio.file.Path

class DirectorySpec : ChildrenOwnerSpec {

  private val children = hashMapOf<String, ContentSpec>()

  override fun addChild(name: String, spec: ContentSpec) {
    check(!name.toSystemIndependentName().contains('/')) { "only simple child names are allowed: $name" }
    check(name !in children) { "'$name' was already added" }
    children[name] = spec
  }

  override fun generate(target: Path) {
    target.createDir()
    for ((name, spec) in children) {
      val childFile = target.resolve(name)
      spec.generate(childFile)
    }
  }

  override fun toString(): String {
    return children.map { (name, content) ->
      if (content is DirectorySpec) {
        "$name: $content"
      } else {
        name
      }
    }.joinToString(prefix = "[", postfix = "]")
  }
}