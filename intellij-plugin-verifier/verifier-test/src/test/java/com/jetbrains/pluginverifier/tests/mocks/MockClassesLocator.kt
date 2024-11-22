/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.FixedClassesResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.classes.locator.ClassesLocator
import com.jetbrains.plugin.structure.intellij.classes.locator.LocationKey
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import org.objectweb.asm.tree.ClassNode
import java.nio.file.Path

class MockClassesLocator(private val name: String, private val classes: List<ClassNode>) : ClassesLocator {

  override val locationKey: LocationKey
    get() = MockLocationKey(name, this)

  private val origin = object : FileOrigin {
    override val parent = null
  }

  override fun findClasses(idePlugin: IdePlugin, pluginFile: Path) = getClassResolvers()

  fun getClassResolvers(): List<Resolver> = listOf(FixedClassesResolver.create(classes, origin))
}