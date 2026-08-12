package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class IdeManagerImplTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  // ── error cases ──────────────────────────────────────────────────────────────

  @Test
  fun `Rider IDE without any platform plugin descriptor throws InvalidIdeException`() {
    val ideRoot = temporaryFolder.newFolder("RD-233.15026.35").toPath()
    buildDirectory(ideRoot) {
      file("build.txt", "RD-233.15026.35")
      dir("lib") {
        zip("app-client.jar") {
          dir("META-INF") {
            file("some-other.xml", "<root/>")
          }
        }
      }
    }

    val exception = assertThrows(InvalidIdeException::class.java) {
      IdeManagerImpl().createIde(ideRoot)
    }
    val expectedReason =
      "Platform plugins are not found. They must be declared in one of plugin.xml, RiderPlugin.xml, PlatformLangPlugin.xml"
    assertEquals(expectedReason, exception.reason)
    assertTrue(
      "Exception message should embed the IDE path",
      exception.message?.contains("RD-233.15026.35") == true
    )
  }

  // ── sanity: direct declaration works ─────────────────────────────────────────

  @Test
  fun `Rider IDE with RiderPlugin xml directly declaring com_intellij loads successfully`() {
    val ideRoot = temporaryFolder.newFolder("RD-233-valid").toPath()
    buildDirectory(ideRoot) {
      file("build.txt", "RD-233.15026.35")
      dir("lib") {
        zip("app-client.jar") {
          dir("META-INF") {
            file("RiderPlugin.xml", """
              <idea-plugin>
                <id>com.intellij</id>
                <name>IDEA CORE</name>
                <module value="com.intellij.modules.rider"/>
              </idea-plugin>
            """.trimIndent())
          }
        }
      }
    }

    val ide = IdeManagerImpl().createIde(ideRoot)
    assertTrue(ide.bundledPlugins.any { it.pluginId == "com.intellij" })
  }

  // ── regression: Rider 2023.3 structure ──────────────────────────────────────

  /**
   * Rider 2023.3 regression: product.jar/RiderPlugin.xml resolves to id=com.jetbrains.rider.languages
   * (not com.intellij), so the fallback to PlatformLangPlugin.xml in app-client.jar must fire.
   *
   * Both plugins must be present in the resulting IDE.
   */
  @Test
  fun `Rider 233 resolves com_intellij from PlatformLangPlugin xml when RiderPlugin xml has a different id`() {
    val ideRoot = buildRider233Directory(
      riderPluginXmlContent = """
        <idea-plugin>
          <id>com.jetbrains.rider.languages</id>
          <name>Rider Languages</name>
          <module value="com.intellij.modules.rider"/>
        </idea-plugin>
      """.trimIndent()
    )

    val ide = IdeManagerImpl().createIde(ideRoot)
    val pluginIds = ide.bundledPlugins.map { it.pluginId }
    assertTrue("Expected 'com.intellij' among platform plugins: $pluginIds",
      pluginIds.contains("com.intellij"))
    assertTrue("Expected 'com.jetbrains.rider.languages' among platform plugins: $pluginIds",
      pluginIds.contains("com.jetbrains.rider.languages"))
  }

  // ── deduplication: IDEA 2024.3 structure ────────────────────────────────────

  /**
   * In IntelliJ IDEA 2024.3+, both product.jar/plugin.xml and app-client.jar/PlatformLangPlugin.xml
   * declare id=com.intellij. The PlatformLangPlugin.xml is a lang-subset that gets xi:included into
   * the real core descriptor; it must NOT appear as a separate plugin in the IDE.
   *
   * The marker module com.intellij.modules.idea is declared only in product.jar/plugin.xml,
   * so its presence proves the real descriptor won (and was not discarded in favour of the subset).
   */
  @Test
  fun `com_intellij is loaded exactly once and from the real descriptor not from PlatformLangPlugin xml`() {
    val ideRoot = temporaryFolder.newFolder("IU-243-dedup").toPath()
    buildDirectory(ideRoot) {
      file("build.txt", "IU-243.0.0")
      dir("lib") {
        zip("product.jar") {
          dir("META-INF") {
            file("plugin.xml", """
              <idea-plugin>
                <id>com.intellij</id>
                <name>IDEA CORE</name>
                <module value="com.intellij.modules.platform"/>
                <module value="com.intellij.modules.idea"/>
              </idea-plugin>
            """.trimIndent())
          }
        }
        zip("app-client.jar") {
          dir("META-INF") {
            file("PlatformLangPlugin.xml", """
              <idea-plugin>
                <id>com.intellij</id>
                <name>IDEA CORE (lang subset)</name>
                <module value="com.intellij.modules.platform"/>
              </idea-plugin>
            """.trimIndent())
          }
        }
      }
    }

    val ide = IdeManagerImpl().createIde(ideRoot)
    val corePlugins = ide.bundledPlugins.filter { it.pluginId == "com.intellij" }
    assertEquals("com.intellij must be loaded exactly once", 1, corePlugins.size)
    assertTrue(
      "The real descriptor (product.jar/plugin.xml) must win over PlatformLangPlugin.xml; " +
        "com.intellij.modules.idea is declared only in the former",
      corePlugins.single().definedModules.contains("com.intellij.modules.idea")
    )
  }

  // ── dispatch routing ─────────────────────────────────────────────────────────

  /**
   * DispatchingIdeManager must select IdeManagerImpl (not ProductInfoBasedIdeManager)
   * for an IDE that has product-info.json without a layout field and build < 242.
   */
  @Test
  fun `DispatchingIdeManager routes Rider 233 structure through IdeManagerImpl`() {
    val ideRoot = buildRider233Directory(
      riderPluginXmlContent = """
        <idea-plugin>
          <id>com.jetbrains.rider.languages</id>
          <name>Rider Languages</name>
          <module value="com.intellij.modules.rider"/>
        </idea-plugin>
      """.trimIndent()
    )

    val ide = DispatchingIdeManager().createIde(ideRoot)
    assertFalse("IDE must not be ProductInfoBasedIde for a Rider 233 structure",
      ide is ProductInfoBasedIde)
    assertTrue(ide.bundledPlugins.any { it.pluginId == "com.intellij" })
  }

  /**
   * DispatchingIdeManager selects ProductInfoBasedIdeManager for an IDE with a layout field
   * and build >= 243.
   */
  @Test
  fun `DispatchingIdeManager routes modern IDE through ProductInfoBasedIdeManager`() {
    // Use MockIdeBuilder which creates a full IU-242+ layout with product-info.json
    val ideRoot = MockIdeBuilder(temporaryFolder, folderSuffix = "-dispatch").buildIdeaDirectory()
    val ide = DispatchingIdeManager().createIde(ideRoot)
    assertTrue("Modern IDE must be ProductInfoBasedIde", ide is ProductInfoBasedIde)
  }

  // ── private helpers ──────────────────────────────────────────────────────────

  /**
   * Builds a Rider 233-like IDE directory:
   * - build.txt = RD-233.15026.35
   * - product-info.json present but without "layout" (causes IdeManagerImpl routing)
   * - lib/product.jar with META-INF/RiderPlugin.xml containing [riderPluginXmlContent]
   * - lib/app-client.jar with META-INF/PlatformLangPlugin.xml declaring id=com.intellij
   */
  private fun buildRider233Directory(riderPluginXmlContent: String): java.nio.file.Path {
    val ideRoot = temporaryFolder.newFolder("RD-233-layout").toPath()
    buildDirectory(ideRoot) {
      file("build.txt", "RD-233.15026.35")
      file("product-info.json", RIDER_233_PRODUCT_INFO_JSON)
      dir("lib") {
        zip("product.jar") {
          dir("META-INF") {
            file("RiderPlugin.xml", riderPluginXmlContent)
          }
        }
        zip("app-client.jar") {
          dir("META-INF") {
            file("PlatformLangPlugin.xml", """
              <idea-plugin>
                <id>com.intellij</id>
                <name>IDEA CORE</name>
                <module value="com.intellij.modules.platform"/>
                <module value="com.intellij.modules.lang"/>
              </idea-plugin>
            """.trimIndent())
          }
        }
      }
    }
    return ideRoot
  }

  private val RIDER_233_PRODUCT_INFO_JSON = """
    {
      "name": "JetBrains Rider",
      "version": "2023.3.6",
      "versionSuffix": "",
      "buildNumber": "233.15026.35",
      "productCode": "RD",
      "dataDirectoryName": "Rider2023.3",
      "svgIconPath": "bin/rider.svg",
      "productVendor": "JetBrains",
      "launch": [],
      "bundledPlugins": [],
      "modules": [],
      "fileExtensions": []
    }
  """.trimIndent()
}
