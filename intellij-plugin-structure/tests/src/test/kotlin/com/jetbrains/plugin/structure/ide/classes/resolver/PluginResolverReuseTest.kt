/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.classes.resolver

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.emptyClass
import com.jetbrains.plugin.structure.base.utils.newTemporaryFile
import com.jetbrains.plugin.structure.classes.resolvers.LazyJarResolver
import com.jetbrains.plugin.structure.classes.resolvers.ResolutionResult
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.jar.initializedJar
import com.jetbrains.plugin.structure.ide.classes.IdeFileOrigin
import com.jetbrains.plugin.structure.intellij.plugin.Classpath
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.jar.Jar
import com.jetbrains.plugin.structure.mocks.MockIde
import com.jetbrains.plugin.structure.mocks.MockIdePlugin
import com.jetbrains.plugin.structure.mocks.idePlugin
import net.bytebuddy.ByteBuddy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Verifies that [CachingPluginDependencyResolverProvider] reuses plugin resolvers that are still in use
 * after they are evicted from the plugin resolver cache, so their JARs are not indexed again.
 * The test counts distinct resolver, initialized [Jar] and class index objects, not cache hits.
 */
class PluginResolverReuseTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  /**
   * Many checked plugins depend on the same plugin JAR, as in a check-trunk-api run against two IDE targets.
   * The dependency resolvers of the first wave stay strongly reachable while the plugin resolver cache is
   * invalidated, which is a deterministic stand-in for an eviction.
   */
  @Test
  fun `plugin resolver in use is reused after plugin resolver cache eviction`() {
    val sharedJarPath = buildZipFile(temporaryFolder.newTemporaryFile("shared/lib/shared-dependency.jar")) {
      dirs("com/example/common") {
        file("Shared.class", ByteBuddy().emptyClass("com.example.common.Shared"))
      }
    }
    val sharedPlugin = MockIdePlugin(
      pluginId = "com.example.shared",
      pluginVersion = "1.0",
      originalFile = sharedJarPath,
      classpath = Classpath.of(listOf(sharedJarPath))
    )
    val release = CachingPluginDependencyResolverProvider(ide("IU-243.100", "release-ide", sharedPlugin))
    val trunk = CachingPluginDependencyResolverProvider(ide("IU-244.100", "trunk-ide", sharedPlugin))
    val checkedPlugins = (1..128).map { idePlugin("com.example.checked.$it") { depends(sharedPlugin) } }
    val workers = Executors.newFixedThreadPool(16)
    try {
      val firstWave = checkedPlugins.take(64)
      val secondWave = checkedPlugins.drop(64)

      val releaseFirst = resolveConcurrently(workers, release, firstWave, sharedJarPath)
      val trunkFirst = resolveConcurrently(workers, trunk, firstWave, sharedJarPath)
      assertEquals(1, jarIdentities(releaseFirst).jars.size)
      assertEquals(1, jarIdentities(trunkFirst).jars.size)

      // Keep the first wave reachable while forcing the eviction scenario.
      // Rebuilding the evicted plugin resolver would index exactly the same plugin JAR again.
      release.invalidatePluginResolverCache()
      trunk.invalidatePluginResolverCache()

      val releaseAll = jarIdentities(releaseFirst + resolveConcurrently(workers, release, secondWave, sharedJarPath))
      val trunkAll = jarIdentities(trunkFirst + resolveConcurrently(workers, trunk, secondWave, sharedJarPath))

      assertEquals("release must reuse the evicted plugin resolver that is still in use", 1, releaseAll.resolvers.size)
      assertEquals("trunk must reuse the evicted plugin resolver that is still in use", 1, trunkAll.resolvers.size)
      assertEquals("release must keep a single Jar of the plugin JAR", 1, releaseAll.jars.size)
      assertEquals("release must keep a single class index of the plugin JAR", 1, releaseAll.indexes.size)
      assertEquals("trunk must keep a single Jar of the plugin JAR", 1, trunkAll.jars.size)
      assertEquals("trunk must keep a single class index of the plugin JAR", 1, trunkAll.indexes.size)
      assertNotSame("distinct IDE providers do not share plugin resolvers", releaseAll.jars.single(), trunkAll.jars.single())
    } finally {
      workers.shutdownNow()
      workers.awaitTermination(30, TimeUnit.SECONDS)
    }
  }

  private fun ide(version: String, directory: String, vararg bundledPlugins: IdePlugin) =
    MockIde(IdeVersion.createIdeVersion(version), temporaryFolder.newFolder(directory).toPath(), bundledPlugins.toList())

  private fun resolveConcurrently(
    workers: ExecutorService,
    provider: CachingPluginDependencyResolverProvider,
    checkedPlugins: List<IdePlugin>,
    sharedJarPath: Path
  ): List<Resolver> {
    val futures = checkedPlugins.map { plugin ->
      workers.submit(Callable<Resolver> {
        val resolver = provider.getResolver(plugin)
        assertTrue("Must resolve the shared plugin class", resolver.containsClass("com/example/common/Shared"))
        val sharedClass = resolver.resolveClass("com/example/common/Shared") as ResolutionResult.Found
        assertEquals(sharedJarPath, (sharedClass.fileOrigin as IdeFileOrigin.BundledPlugin).pluginFile)
        val methods = sharedClass.value.methods
        assertTrue("Plugin dependencies are read in SIGNATURES mode", methods.isNotEmpty() && methods.all { it.instructions.size() == 0 })
        resolver
      })
    }
    return futures.map { it.get(60, TimeUnit.SECONDS) }
  }

  private class JarIdentities(val resolvers: Set<LazyJarResolver>, val jars: Set<Jar>, val indexes: Set<Any>)

  private fun jarIdentities(dependencyResolvers: List<Resolver>): JarIdentities {
    val resolvers = identitySet<LazyJarResolver>()
    val jars = identitySet<Jar>()
    val indexes = identitySet<Any>()
    val indexField = Jar::class.java.getDeclaredField("classesInJar").apply { isAccessible = true }
    val componentsField = CachingPluginDependencyResolverProvider.DependencyTreeAwareResolver::class.java
      .getDeclaredField("components").apply { isAccessible = true }
    for (dependencyResolver in dependencyResolvers) {
      val components = componentsField.get(dependencyResolver) as Collection<*>
      for (jarResolver in components.filterIsInstance<LazyJarResolver>()) {
        val jar = jarResolver.initializedJar()
        resolvers += jarResolver
        jars += jar
        indexes += indexField.get(jar)
      }
    }
    return JarIdentities(resolvers, jars, indexes)
  }

  private fun <T> identitySet(): MutableSet<T> = Collections.newSetFromMap(IdentityHashMap())
}
