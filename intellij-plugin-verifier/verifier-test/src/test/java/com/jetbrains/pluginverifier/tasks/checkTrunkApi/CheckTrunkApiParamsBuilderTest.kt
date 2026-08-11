package com.jetbrains.pluginverifier.tasks.checkTrunkApi

import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.cache.CacheStatistics
import com.jetbrains.pluginverifier.tasks.createDependencyFinder
import com.jetbrains.pluginverifier.tasks.createRepository
import com.jetbrains.pluginverifier.tests.mocks.MockIde
import com.jetbrains.pluginverifier.tests.mocks.MockPluginRepositoryAdapter
import com.jetbrains.pluginverifier.tests.mocks.createMockPluginInfo
import com.jetbrains.pluginverifier.tests.mocks.createPluginArchiveManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CheckTrunkApiParamsBuilderTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var pluginArchiveManager: PluginArchiveManager
  private lateinit var pluginDetailsCache: RecordingPluginDetailsCache
  private lateinit var pluginRepository: PluginRepository

  @Before
  fun setUp() {
    pluginArchiveManager = temporaryFolder.createPluginArchiveManager()
    pluginDetailsCache = RecordingPluginDetailsCache()

    val marketplaceFoo = createMockPluginInfo("foo", "9.0-marketplace")
    val marketplaceBar = createMockPluginInfo("bar", "2.0-marketplace")
    pluginRepository = object : MockPluginRepositoryAdapter() {
      override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String): PluginInfo? = when (pluginId) {
        "foo" -> marketplaceFoo
        "bar" -> marketplaceBar
        else -> null
      }
    }

  }

  @Test
  fun `online check trunk prefers local plugins and falls back to public repository`() {
    val localRepositoryRoot = temporaryFolder.newFolder("local-plugins")
    createLocalPlugin(localRepositoryRoot, "foo", "1.0-local")

    val opts = CmdOpts().apply {
      offlineMode = false
    }
    val localRepository = createRepository(localRepositoryRoot.absolutePath, opts, pluginArchiveManager)

    // This assertion is the direct regression check: before the fix, online mode
    // replaced -rjbp/-tjbp with EmptyPluginRepository.
    assertEquals("1.0-local", localRepository.getAllVersionsOfPlugin("foo").single().version)

    val ide = MockIde(IdeVersion.createIdeVersion("IU-262.1"))
    val finder = createDependencyFinder(ide, ide, pluginRepository, localRepository, pluginDetailsCache)

    finder.findPluginDependency("foo", isModule = false).close()
    finder.findPluginDependency("bar", isModule = false).close()

    // foo exists in both repositories, so the local artifact must win.
    // bar does not exist locally, so resolution must continue to the public repository.
    assertEquals(
      listOf("1.0-local", "2.0-marketplace"),
      pluginDetailsCache.requestedPlugins.map { it.version }
    )
  }

  private fun createLocalPlugin(repositoryRoot: File, pluginId: String, version: String) {
    val metaInf = File(repositoryRoot, "$pluginId/META-INF")
    check(metaInf.mkdirs())
    File(metaInf, "plugin.xml").writeText(
      """
        <idea-plugin>
          <id>$pluginId</id>
          <name>$pluginId</name>
          <version>$version</version>
          <vendor email="test@jetbrains.com" url="https://www.jetbrains.com">JetBrains</vendor>
          <description>This is a sufficiently long plugin description for the verifier test.</description>
          <change-notes>These are sufficiently long change notes for the verifier test.</change-notes>
          <idea-version since-build="262.1" until-build="262.*"/>
          <depends>com.intellij.modules.platform</depends>
        </idea-plugin>
      """.trimIndent()
    )
  }

  @After
  fun tearDown() {
    pluginArchiveManager.close()
  }

  private class RecordingPluginDetailsCache : PluginDetailsCache {
    val requestedPlugins = mutableListOf<PluginInfo>()

    override val statistics: CacheStatistics = CacheStatistics()

    override fun getPluginDetailsCacheEntry(pluginInfo: PluginInfo): PluginDetailsCache.Result {
      requestedPlugins += pluginInfo
      return PluginDetailsCache.Result.FileNotFound("File is not needed for dependency selection test")
    }

    override fun close() = Unit
  }
}
