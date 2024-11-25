package com.jetbrains.plugin.structure.ide.classes

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.classes.resolvers.Resolver.ReadMode
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.ide.InvalidIdeException
import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode
import org.intellij.lang.annotations.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class IdeResolverCreatorTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `layout component with missing file in the filesystem is skipped`() {
    val ideResolver = IdeResolverCreator.createIdeResolver(createIdeWithNonExistentFileOfLayoutComponent())
    assertEquals("IDE Resolver class count", 0, ideResolver.allClasses.size)
    assertEquals("IDE Resolver package count", 0, ideResolver.allPackages.size)
  }

  @Test
  fun `layout component with missing file in the filesystem fails the whole IDE`() {
    val resolverConfiguration = IdeResolverConfiguration(ReadMode.FULL, MissingLayoutFileMode.FAIL)
    assertThrows("IDE has invalid layout", InvalidIdeException::class.java) {
      IdeResolverCreator.createIdeResolver(createIdeWithNonExistentFileOfLayoutComponent(), resolverConfiguration)
    }
  }

  private fun createIdeWithNonExistentFileOfLayoutComponent(): Ide {
    val ideVersion = "IU-251.7539"
    val ideRoot: Path = temporaryFolder.newFolder("idea").toPath()

    buildDirectory(ideRoot) {
      file("build.txt", ideVersion)
      file("product-info.json", productInfoJson)
      dir("lib") {
        zip("product.jar") {
          dir("META-INF") {
            file("plugin.xml", ideaCorePluginXml)
          }
        }
      }
      dir("modules") {
        zip("module-descriptors.jar") { /* intentionally left blank */ }
      }
    }

    return IdeManager.createManager().createIde(ideRoot)
  }

  @Language("JSON")
  private val productInfoJson = """
    {
      "name": "IntelliJ IDEA",
      "version": "2025.1",
      "versionSuffix": "EAP",
      "buildNumber": "251.7539",
      "productCode": "IU",
      "envVarBaseName": "IDEA",
      "dataDirectoryName": "IntelliJIdea2025.1",
      "svgIconPath": "bin/idea.svg",
      "productVendor": "JetBrains",
      "launch": [],
      "bundledPlugins": [],
      "modules": [],
      "fileExtensions": [],
      "layout": [
        {
          "name": "intellij.qodana.sarif",
          "kind": "moduleV2",
          "classPath": [
            "plugins/qodana/lib/modules/intellij.qodana.sarif.jar"
          ]
        }
      ]
    }        
      """.trimIndent()

  private val ideaCorePluginXml: String
    @Language("JSON")
    get() = """
    <idea-plugin xmlns:xi="http://www.w3.org/2001/XInclude">
      <id>com.intellij</id>
      <name>IDEA CORE</name>
    </idea-plugin>        
    """.trimIndent()
}