/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.intellij.plugin.createZip
import com.jetbrains.plugin.structure.intellij.plugin.loaders.JarPluginLoader.Loadability.Loadable
import com.jetbrains.plugin.structure.intellij.plugin.loaders.JarPluginLoader.Loadability.NotLoadable
import com.jetbrains.plugin.structure.intellij.problems.IntelliJPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.DefaultResourceResolver
import com.jetbrains.plugin.structure.jar.CachingJarFileSystemProvider
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class JarPluginLoaderTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var jarFileSystemProvider: CachingJarFileSystemProvider

  @Before
  fun setUp() {
    this.jarFileSystemProvider = CachingJarFileSystemProvider()
  }

  @Test
  fun `JAR contains a descriptor`() {
    val loader = JarPluginLoader(jarFileSystemProvider)
    val pluginJar = newJar()
    val descriptorPath = "plugin.xml"
    createZip(pluginJar, mapOf("META-INF/plugin.xml" to "<idea-plugin />"))

    val context = JarPluginLoader.Context(
      pluginJar,
      descriptorPath,
      validateDescriptor = true,
      resourceResolver = DefaultResourceResolver,
      parentPlugin = null,
      problemResolver = IntelliJPluginCreationResultResolver()
    )

    assertTrue(loader.getLoadability(context) is Loadable)
  }

  @Test
  fun `JAR contains a descriptor with slash-based path which is against the ZIP specs`() {
    val loader = JarPluginLoader(jarFileSystemProvider)
    val pluginJar = newJar()
    val descriptorPath = "plugin.xml"
    createZip(pluginJar, mapOf("META-INF\\plugin.xml" to "<idea-plugin />"))

    val context = JarPluginLoader.Context(
      pluginJar,
      descriptorPath,
      validateDescriptor = true,
      resourceResolver = DefaultResourceResolver,
      parentPlugin = null,
      problemResolver = IntelliJPluginCreationResultResolver()
    )

    assertFalse(loader.getLoadability(context) is Loadable)
  }

  @Test
  fun `JAR contains a descriptor with relative path`() {
    val loader = JarPluginLoader(jarFileSystemProvider)
    val pluginJar = newJar()
    val descriptorPath = "../module.xml"
    createZip(pluginJar, mapOf(
      "module.xml" to "<idea-plugin />"
    ))

    val context = JarPluginLoader.Context(
      pluginJar,
      descriptorPath,
      validateDescriptor = true,
      resourceResolver = DefaultResourceResolver,
      parentPlugin = null,
      problemResolver = IntelliJPluginCreationResultResolver()
    )

    assertTrue(loader.getLoadability(context) is Loadable)
  }

  @Test
  fun `JAR contains a descriptor with relative path and backslash`() {
    val loader = JarPluginLoader(jarFileSystemProvider)
    val pluginJar = newJar()
    val descriptorPath = "..\\module.xml"
    createZip(pluginJar, mapOf(
      "module.xml" to "<idea-plugin />"
    ))

    val context = JarPluginLoader.Context(
      pluginJar,
      descriptorPath,
      validateDescriptor = true,
      resourceResolver = DefaultResourceResolver,
      parentPlugin = null,
      problemResolver = IntelliJPluginCreationResultResolver()
    )

    assertTrue(loader.getLoadability(context) is Loadable)
  }

  @Test
  fun `JAR does not contain a descriptor`() {
    val loader = JarPluginLoader(jarFileSystemProvider)
    val pluginJar = newJar()
    createZip(pluginJar, mapOf("README.txt" to "This is not a plugin ZIP"))

    val context = JarPluginLoader.Context(
      pluginJar,
      "META-INF/plugin.xml",
      validateDescriptor = true,
      resourceResolver = DefaultResourceResolver,
      parentPlugin = null,
      problemResolver = IntelliJPluginCreationResultResolver()
    )

    assertTrue(loader.getLoadability(context) is NotLoadable)
  }

  private fun newJar(jarName: String = "plugin.jar"): Path {
    return temporaryFolder.newFile(jarName).toPath()
  }

  @After
  fun tearDown() {
    jarFileSystemProvider.close()
  }
}