package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.ide.IntelliJPlatformProduct.RIDER
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

private val IDE_VERSION = IdeVersion.createIdeVersion("RD-242.19533.58")

class MockRiderBuilder (private val temporaryFolder: TemporaryFolder, private val ideVersion: IdeVersion = IDE_VERSION) {
  private val ideRoot: Path by lazy {
    temporaryFolder.newFolder(ideVersion.asString()).toPath()
  }

  private val platformPrefix: String = (IntelliJPlatformProduct.fromIdeVersion(ideVersion)?: RIDER).platformPrefix

  fun buildIdeaDirectory() = buildDirectory(ideRoot) {
    file("build.txt", ideVersion.asString())
    file("product-info.json", productInfoJson())
    dir("lib") {
      zip("app-client.jar") {
        dir("META-INF") {
          file("${platformPrefix}Plugin.xml", platformLangPluginXml)
        }
      }
    }
    dir("modules") {
      zip("module-descriptors.jar") {
        // intentionally left empty as there are no modules
      }
    }
  }

  private fun productInfoJson(): String {
    return """
        {
          "name": "JetBrains Rider",
          "version": "2024.2",
          "versionSuffix": "EAP 5",
          "buildNumber": "242.19533.58",
          "productCode": "RD",
          "dataDirectoryName": "Rider2024.2",
          "svgIconPath": "bin/rider.svg",
          "productVendor": "JetBrains",
          "launch": [],
          "customProperties": [],
          "bundledPlugins": [],
          "modules": [
            "com.intellij.modules.rider"
          ],
          "fileExtensions": [],
          "layout": [
            {
              "name": "com.intellij.modules.rider",
              "kind": "pluginAlias"
            }
          ]
        }
    """.trimIndent()
  }

  private val platformLangPluginXml: String
    get() = """
    <idea-plugin xmlns:xi="http://www.w3.org/2001/XInclude">
      <id>com.intellij</id>
      <name>IDEA CORE</name>
    
      <module value="com.intellij.modules.rider"/>
    </idea-plugin>        
    """.trimIndent()
}