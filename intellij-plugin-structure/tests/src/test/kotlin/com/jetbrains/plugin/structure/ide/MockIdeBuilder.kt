package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class MockIdeBuilder(private val temporaryFolder: TemporaryFolder) {
  private val ideRoot: Path by lazy {
    temporaryFolder.newFolder("idea").toPath()
  }

  fun buildIdeaDirectory() = buildDirectory(ideRoot) {
    file("build.txt", "IU-242.10180.25")
    file("product-info.json", productInfoJson())
    dir("lib") {
      zip("app-client.jar") {
        dir("META-INF") {
          file("PlatformLangPlugin.xml", platformLangPluginXml)
        }
      }
      dir("modules") {
        zip("intellij.notebooks.ui.jar") {
          file("intellij.notebooks.ui.xml") {
            """
                  <idea-plugin package="org.jetbrains.plugins.notebooks.ui">
                  </idea-plugin>
                """.trimIndent()
          }
        }
        zip("intellij.notebooks.visualization.jar") {
          file("intellij.notebooks.visualization.xml") {
            """
                <idea-plugin package="org.jetbrains.plugins.notebooks.visualization">
                  <module value="com.intellij.modules.notebooks.visualization" />
                  <dependencies>
                    <module name="intellij.notebooks.ui"/>
                  </dependencies>

                  <extensions defaultExtensionNs="com.intellij">
                    <notificationGroup displayType="BALLOON" id="Notebook Table" bundle="messages.VisualizationBundle" key="inlay.output.table.notification.group.name"/>
                  </extensions>

                </idea-plugin>
                """.trimIndent()
          }
        }
      }
    }
    dir("modules") {
      zip("module-descriptors.jar") {
        file("intellij.notebooks.ui.xml") {
          """
                <?xml version="1.0" encoding="UTF-8"?>
                <module name="intellij.notebooks.ui">
                  <dependencies>
                    <module name="intellij.platform.lang"/>
                  </dependencies>
                  <resources>
                    <resource-root path="../lib/modules/intellij.notebooks.ui.jar"/>
                  </resources>
                </module>                
              """.trimIndent()
        }
        file("intellij.notebooks.visualization.xml") {
          """
                <?xml version="1.0" encoding="UTF-8"?>
                <module name="intellij.notebooks.visualization">
                  <dependencies>
                    <module name="intellij.notebooks.ui"/>
                  </dependencies>
                  <resources>
                    <resource-root path="../lib/modules/intellij.notebooks.visualization.jar"/>
                  </resources>
                </module>                
              """.trimIndent()
        }
        file("intellij.java.featuresTrainer.xml") {
          """
              <?xml version="1.0" encoding="UTF-8"?>
              <module name="intellij.java.featuresTrainer">
                <dependencies>
                </dependencies>
                <resources>
                  <resource-root path="../plugins/java/lib/modules/intellij.java.featuresTrainer.jar"/>
                </resources>
              </module>            
          """.trimIndent()
        }
      }
    }
    dir("plugins") {
      dir("java") {
        dir("lib") {
          dir("modules") {
            zip("intellij.java.featuresTrainer.jar") {
              file("intellij.java.featuresTrainer.xml") {
                """
                  <idea-plugin>
                    <extensions defaultExtensionNs="com.intellij">
                          <lang.documentationProvider 
                               language="JAVA" 
                              implementationClass="com.intellij.java.featuresTrainer.onboarding.tips.JavaOnboardingTipsDocumentationProvider"
                          />
                    </extensions>
                  </idea-plugin>                
                """.trimIndent()
              }
            }
          }
        }
      }
    }
  }

  private fun productInfoJson(): String {
    return """
        {
          "name": "IntelliJ IDEA",
          "version": "2024.2",
          "versionSuffix": "EAP",
          "buildNumber": "242.10180.25",
          "productCode": "IU",
          "dataDirectoryName": "IntelliJIdea2024.2",
          "svgIconPath": "bin/idea.svg",
          "productVendor": "JetBrains",
          "launch": [],
          "bundledPlugins": [],
          "modules": [],
          "fileExtensions": [],
          "layout": [
            {
              "name": "intellij.notebooks.ui",
              "kind": "productModuleV2",
              "classPath": [
                "lib/modules/intellij.notebooks.ui.jar"
              ]
            },
            {
              "name": "intellij.notebooks.visualization",
              "kind": "productModuleV2",
              "classPath": [
                "lib/modules/intellij.notebooks.visualization.jar"
              ]
            },
            {
              "name": "intellij.java.featuresTrainer",
              "kind": "moduleV2",
              "classPath": [
                "plugins/java/lib/modules/intellij.java.featuresTrainer.jar"
              ]
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
    
      <module value="com.intellij.modules.platform"/>
      <module value="com.intellij.modules.lang"/>
      <module value="com.intellij.modules.xdebugger"/>
      <module value="com.intellij.modules.externalSystem"/>
    </idea-plugin>        
    """.trimIndent()
}