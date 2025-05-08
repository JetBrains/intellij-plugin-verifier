/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.mocks.modify
import com.jetbrains.plugin.structure.mocks.perfectXmlBuilder
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

data class CompiledIdeDirectories(val idePath: Path, val m2Path: Path)

/**
 * .m2 (local Maven repository)
 *   com
 *     some
 *       lib-plugin
 *         1.0
 *           lib-plugin-1.0.jar
 *             META-INF
 *               lib-plugin.xml
 *
 * idea/
 *   build.txt (IU-163.1.2.3)
 *   .idea/
 *   community/.idea
 *   out/
 *     classes/
 *       production/
 *         somePlugin/
 *           META-INF/
 *             plugin.xml
 *                references someTheme.theme.json
 *                x-includes "lib-plugin.xml", which resides in Maven repository dependency (com.some:lib-plugin:1.0)
 *         themeHolder
 *           someTheme.theme.json
 */
fun createCompiledIdeDirectories(temporaryFolder: TemporaryFolder): CompiledIdeDirectories {
  val m2Directory = buildDirectory(temporaryFolder.newFolder(".m2").toPath()) {
    dir("com") {
      dir("some") {
        dir("lib-plugin") {
          dir("1.0") {
            zip("lib-plugin-1.0.jar") {
              dir("META-INF") {
                file("lib-plugin.xml") {
                  """
                      <idea-plugin>
                         <extensions defaultExtensionNs="com.intellij">
                            <someExt someKey="someValue"/>
                          </extensions>
                      </idea-plugin>
                    """.trimIndent()
                }
              }
              dirs("com/example") {
                file(
                  "LibPluginService.class",
                  createEmptyClass("com/example/LibPluginService")
                )
              }
            }
          }
        }
      }
    }
  }
  val mavenRepository = m2Directory.toAbsolutePath()
  System.setProperty("MAVEN_REPOSITORY", mavenRepository.toString())

  val ideaFolder = buildDirectory(temporaryFolder.newFolder("idea").toPath()) {
    file("build.txt", "IU-163.1.2.3")
    dir(".idea") {
      file("modules.xml") {
        """
            <project version="4">
              <component name="ProjectModuleManager">
                <modules>
                  <module fileurl="file://${'$'}PROJECT_DIR${'$'}/somePlugin/somePlugin.iml" filepath="${'$'}PROJECT_DIR${'$'}/somePlugin/somePlugin.iml"/>
                </modules>
              </component>
            </project>
          """.trimIndent()
      }
    }

    dir("community") {
      dir(".idea") { }
    }

    dir("somePlugin") {
      file("somePlugin.iml") {
        """
            <module type="JAVA_MODULE" version="4">
              <component name="NewModuleRootManager" inherit-compiler-output="true">
                <exclude-output />
                <content url="file://${'$'}PROJECT_DIR${'$'}/somePlugin">
                  <sourceFolder url="file://${'$'}PROJECT_DIR${'$'}/somePlugin/src" isTestSource="false" />
                </content>
                <orderEntry type="inheritedJdk" />
                <orderEntry type="sourceFolder" forTests="false" />
                
                <orderEntry type="module-library" scope="RUNTIME">
                  <library name="lib-plugin" type="repository">
                    <properties maven-id="com.some:lib-plugin:1.0" />
                    <CLASSES>
                      <root url="jar://$mavenRepository/com/some/lib-plugin/1.0/lib-plugin-1.0.jar!/" />
                    </CLASSES>
                  </library>
                </orderEntry>
              </component>
            </module>
          """.trimIndent()
      }
    }

    dir("out") {
      dir("classes") {
        dir("production") {
          dir("somePlugin") {
            dir("META-INF") {
              file("plugin.xml") {
                perfectXmlBuilder
                  .modify {
                    ideaPluginTagOpen = """<idea-plugin xmlns:xi="http://www.w3.org/2001/XInclude">"""

                    additionalContent = """
                          <extensions defaultExtensionNs="com.intellij">
                            <themeProvider id="someId" path="/someTheme.theme.json"/>
                          </extensions>
                          
                          <xi:include href="/META-INF/lib-plugin.xml" xpointer="xpointer(/idea-plugin/*)"/>
                        """.trimIndent()
                  }
              }
            }
            dirs("com/example/somePlugin") {
              file(
                "SomePluginService.class",
                createEmptyClass("com/example/somePlugin/SomePluginService")
              )
            }
          }

          dir("themeHolder") {
            file("someTheme.theme.json") {
              """
                {
                  "name": "someTheme",
                  "dark": true
                }
                """.trimIndent()
            }
          }
        }
      }
    }
  }

  return CompiledIdeDirectories(ideaFolder, m2Directory)
}