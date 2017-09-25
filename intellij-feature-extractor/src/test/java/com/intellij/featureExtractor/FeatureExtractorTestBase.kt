package com.intellij.featureExtractor

import com.jetbrains.plugin.structure.classes.resolvers.ClassFilesResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import org.junit.After
import org.junit.Before
import org.objectweb.asm.tree.ClassNode
import java.io.File

open class FeatureExtractorTestBase {

  protected lateinit var resolver: Resolver

  @Before
  fun setUp() {
    resolver = ClassFilesResolver(File("."))
  }

  @After
  fun tearDown() {
    resolver.close()
  }

  fun readClassNode(className: String): ClassNode = resolver.findClass(className.replace('.', '/'))!!

}