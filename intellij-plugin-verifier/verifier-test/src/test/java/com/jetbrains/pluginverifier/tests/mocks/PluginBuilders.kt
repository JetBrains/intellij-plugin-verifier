package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentSpec
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectoryContent
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import org.junit.Assert.assertEquals
import java.nio.file.Path

internal data class IdeaPluginSpec(val id: String, val vendor: String)

internal fun Path.buildIdePlugin(pluginContentBuilder: (ContentBuilder).() -> Unit): IdePlugin {
  val pluginFile = buildZipFile(zipFile = this) {
    pluginContentBuilder()
  }
  return (IdePluginManager.createManager().createPlugin(pluginFile) as PluginCreationSuccess).plugin
}

/**
 * Builds a bare-bones IDE without any plugins except the core plugin.
 *
 * This plugin exposes the `com.intellij.modules.platform` module
 * that represents a shared functionality.
 *
 * @receiver a path where the IDE will reside, usually with a `idea` name.
 */
internal fun Path.buildCoreIde(): Ide {
  val ideaDirectory = buildDirectory(directory = this) {
    file("build.txt", "IU-192.1")
    dir("lib") {
      zip("idea.jar") {
        dir("META-INF") {
          file("plugin.xml") {
            """
                <idea-plugin>
                  <id>com.intellij</id>
                  <name>IDEA CORE</name>
                  <version>1.0</version>
                  <module value="com.intellij.modules.platform"/>                
                </idea-plugin>
                """.trimIndent()
          }
        }
      }
    }
  }

  val ide = IdeManager.createManager().createIde(ideaDirectory)
  assertEquals("IU-192.1", ide.version.asString())
  return ide
}

internal fun bundledPlugin(s: PluginSpec.() -> Unit): PluginSpec {
  val builder = PluginSpec()
  s(builder)
  return builder
}

internal class PluginSpec {
  var id: String = "simple-plugin"

  /**
   * Plugin artifact name. Mapped to directory name or JAR name (without `.jar` suffix).
   */
  var artifactName: String? = null

  var descriptorContent: String = """
                    <idea-plugin>
                      <id>$id</id>
                      <module value="com.intellij.modules.all" />
                    </idea-plugin>
                    """.trimIndent()

  var classContentBuilder: (ContentBuilder.() -> Unit)? = null

  private val dirName: String
    get() = artifactName?.let { return it } ?: id

  private val jarName: String
    get() = "$dirName.jar"

  fun build(): ContentSpec = buildDirectoryContent {
    dir(dirName) {
      dir("lib") {
        zip("${artifactName}.jar") {
          dir("META-INF") {
            file("plugin.xml", descriptorContent)
          }
        }
      }
    }
  }

  fun build(contentBuilder: ContentBuilder) = with(contentBuilder) {
    dir(dirName) {
      dir("lib") {
        buildJar(this)
      }
    }
  }

  internal fun buildJar(contentBuilder: ContentBuilder) = with(contentBuilder) {
    zip(jarName) {
      dir("META-INF") {
        file("plugin.xml", descriptorContent)
      }
      classContentBuilder?.invoke(this)
    }
  }
}