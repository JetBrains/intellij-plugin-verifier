package com.intellij.featureExtractor

import com.intellij.structure.impl.resolvers.FilesResolver
import com.intellij.structure.resolvers.Resolver
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.junit.After
import org.junit.Before
import java.io.File

open class FeatureExtractorTestBase {

  protected lateinit var resolver: Resolver

  @Before
  fun setUp() {
    resolver = FilesResolver("Test class files", File("."))
  }

  @After
  fun tearDown() {
    resolver.close()
  }

  fun readClassNode(className: String): ClassNode = resolver.findClass(className.replace('.', '/'))

}