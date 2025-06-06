package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.ide.ProductInfoBasedIdeManager
import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode
import com.jetbrains.plugin.structure.intellij.plugin.EventLogSinglePluginProvider
import com.jetbrains.plugin.structure.intellij.plugin.EventLogSinglePluginProvider.LogEntry
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.mocks.modify
import com.jetbrains.plugin.structure.mocks.perfectXmlBuilder
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path
import java.nio.file.Paths

private const val HEADER = """
      <id>someId</id>
      <name>someName</name>
      <version>someVersion</version>
      <vendor email="vendor.com" url="url">vendor</vendor>
      <description>this description is looooooooooong enough</description>
      <change-notes>these change-notes are looooooooooong enough</change-notes>
      <idea-version since-build="131.1"/>
    """

class DependenciesTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private fun buildIdeaDirectory(): Path = temporaryFolder.newFolder("idea").toPath().let { ideRoot ->
    buildDirectory(ideRoot) {
      file("build.txt", "IU-242.10180.25")
      dir("lib") {
        zip("app.jar") {
          dir("META-INF") {
            file(comIntellijPlugin)
            file(javaIdePlugin)
            file(platformLangPluginXml)
            file(vcsXml)
            file(coverageCommonPluginXml)
            file(duplicateDetector)
            file(structuralSearch)
          }
          file(intellijJson)
        }
        zip("modules.jar") {
          file(intellijPlatformIdeNewUiOnboardingXml)
          file(intellijProfilerAsyncOne)
          file(intellijProfilerUltimateIdeaAsyncProfiler)
          file(intellijProfilerCommon)
          file(intellijProfilerUltimate)
          file(intellijNotebooksVisualization)
          file(intellijNotebooksUi)
          file(intellijPlatformImagesCopyright)
          file(intellijXmlBeans)
          file(intellijExecutionProcessElevation)
          file(intellijExecutionProcessClient)
          file(intellijExecutionProcessCommon)
          file(intellijExecutionProcessDaemon)
        }
        zip("product.jar") {
          dir("META-INF") {
            file(ultimate)
            file(microservices)
            file(sshAttach)
          }
        }
        zip("app-client.jar") {
          file(intellijPlatformSsh)
          file(intellijPlatformSshUi)
          file(intellijPlatformRemoteServersImpl)
          dir("META-INF") {
            file(xmlPlugin)
          }

        }
      }
      dir("plugins") {
        dir("vcs-git") {
          dir("lib") {
            zip("vcs-git.jar") {
              dir("META-INF") {
                file(git4IdeaXml)
              }
              file(intellijVcGitNewUiOnboardingXml)
            }
          }
        }
        dir("copyright") {
          dir("lib") {
            zip("copyright.jar") {
              dir("META-INF") {
                file(comIntellijCopyright)
              }
            }
          }
        }
        dir("java") {
          dir("lib") {
            zip("java-impl.jar") {
              dir("META-INF") {
                file(designerCorePlugin)
              }
            }
          }
        }
        dir("platform-images") {
          dir("lib") {
            zip("platform-images.jar") {
              dir("META-INF") {
                file(comIntellijPlatformImages)
              }
            }
          }
        }
        dir("performanceTesting") {
          dir("lib") {
            zip("performanceTesting.jar") {
              dir("META-INF") {
                file(comJetbrainsPerformancePlugin)
              }
            }
          }
        }
      }
    }
  }

  private fun ContentBuilder.file(xmlDefinition: Descriptor) {
    file(xmlDefinition.name, xmlDefinition.xml)
  }

  private val comIntellijPlugin = Xml("lib/app.jar#META-INF", "plugin") {
    //language=XML
    """
    <idea-plugin xmlns:xi="http://www.w3.org/2001/XInclude">
      <id>com.intellij</id>
    
      <module value="com.intellij.modules.idea"/>
      <module value="com.intellij.modules.idea.ultimate"/>
    
      <module value="com.intellij.modules.java-capable"/>
      <module value="com.intellij.modules.php-capable"/>
      <module value="com.intellij.modules.ruby-capable"/>
      <module value="com.intellij.modules.python-pro-capable"/>
      <module value="com.intellij.modules.python-in-non-pycharm-ide-capable"/> <!-- Enable Non-Pycharm-IDE support in Python plugin -->
      <module value="com.intellij.modules.c-capable"/>
      <module value="com.intellij.modules.go-capable"/>
    
      <module value="com.intellij.modules.nativeDebug-plugin-capable"/>
      <module value="com.intellij.modules.clion-plugin-capable"/>
      <module value="com.intellij.modules.rust-capable"/>
    
      <!-- turns on "Run Targets" specifically for IDEA Ultimate -->
      <module value="com.intellij.modules.run.targets"/>
    
      <content>
        <module name="intellij.profiler.asyncOne"/>
        <module name="intellij.profiler.ultimate.ideaAsyncProfiler"/>
        <module name="intellij.profiler.common"/>
        <module name="intellij.profiler.ultimate"/>
        <module name="intellij.notebooks.visualization"/>
        <module name="intellij.notebooks.ui"/>
        <module name="intellij.platform.images.copyright"/>
        <module name="intellij.xml.xmlbeans"/>
        <module name="intellij.platform.ide.newUiOnboarding"/>
        <module name="intellij.ide.startup.importSettings"/>
        <module name="intellij.platform.ml.embeddings"/>
        <module name="intellij.kotlin.onboarding-promoter"/>
        <!-- elevation module with dependencies -->
        <module name="intellij.execution.process.elevation"/>
        <module name="intellij.execution.process.mediator.client"/>
        <module name="intellij.execution.process.mediator.common"/>
        <module name="intellij.execution.process.mediator.daemon"/>
      </content>
    
      <xi:include href="/META-INF/JavaIdePlugin.xml">
        <xi:fallback/>
      </xi:include>
      <xi:include href="/META-INF/duplicates.xml">
        <xi:fallback/>
      </xi:include>
      <xi:include href="/META-INF/duplicates-xml.xml">
        <xi:fallback/>
      </xi:include>
      <xi:include href="intellij.platform.ssh.xml">
        <xi:fallback/>
      </xi:include>
      <xi:include href="/META-INF/ssh-attach.xml">
        <xi:fallback/>
      </xi:include>
      <xi:include href="/META-INF/DuplicateDetector.xml">
        <xi:fallback/>
      </xi:include>
      <xi:include href="intellij.xml.duplicatesDetection.xml">
        <xi:fallback/>
      </xi:include>
      <xi:include href="/META-INF/microservices.xml">
        <xi:fallback/>
      </xi:include>
      <xi:include href="/META-INF/ultimate.xml"/>
      <!-- no dependencies, no module declarations 
      <xi:include href="/META-INF/tips-intellij-idea.xml"/>
      <xi:include href="intellij.platform.lsp.xml"/>
      -->
    
    </idea-plugin>
    """
  }

  private val coverageCommonPluginXml = Xml("lib/app.jar#META-INF", "coverage-common-plugin") {
    //language=XML
    """
    <idea-plugin>
      <module value="com.intellij.modules.coverage"/>
    </idea-plugin>    
    """
  }

  private val vcsXml = Xml("lib/app.jar#META-INF", "VCS") {
    //language=XML
    """
      <idea-plugin>
        <module value="com.intellij.modules.vcs"/>
      </idea-plugin>
    """
  }

  // lib/app.jar#META-INF/PlatformLangPlugin.xml
  private val platformLangPluginXml = Xml("lib/app.jar#META-INF", "PlatformLangPlugin") {
    //language=XML
    """
    <!--suppress ALL -->
    <idea-plugin xmlns:xi="http://www.w3.org/2001/XInclude">
      <id>com.intellij</id>
      <name>IDEA CORE</name>
    
      <module value="com.intellij.modules.platform"/>
      <module value="com.intellij.modules.lang"/>
      <module value="com.intellij.modules.xdebugger"/>
      <module value="com.intellij.modules.externalSystem"/>
    
      <!-- should be here, otherwise not loaded in tests (in unit test mode, the platform prefix is `PlatformLangXml) -->
      <content>
        <!-- lib/modules.jar. No dependencies -->
        <module name="intellij.platform.settings.local"/>
      </content>
    
      <xi:include href="/META-INF/coverage-common-plugin.xml">
        <xi:fallback/>
      </xi:include>
    
      <xi:include href="/META-INF/VCS.xml">
        <xi:fallback/>
      </xi:include>

    
    </idea-plugin>      
    """
  }

  private val intellijVcGitNewUiOnboardingXml = Xml("lib/modules.jar", "intellij.vcs.git.newUiOnboarding") {
    //language=XML
    """
    <idea-plugin package="git4idea.newUiOnboarding">
      <dependencies>
        <module name="intellij.platform.ide.newUiOnboarding"/>
      </dependencies>
    </idea-plugin>    
    """
  }

  private val git4IdeaXml = Xml("plugins/vcs-git/lib/vcs-git.jar#META-INF", "plugin") {
    //language=XML
    """
    <idea-plugin package="git4idea">
      <name>Git</name>
      <id>Git4Idea</id>
      <dependencies>
        <plugin id="com.intellij.modules.vcs"/>
      </dependencies>
      <depends optional="true" config-file="git-performance-plugin-support.xml">com.jetbrains.performancePlugin</depends>
      <content>
        <module name="intellij.vcs.git/newUiOnboarding"/>
      </content>
    </idea-plugin>
    """
  }

  private val intellijPlatformIdeNewUiOnboardingXml = Xml("lib/modules.jar", "intellij.platform.ide.newUiOnboarding") {
    //language=XML
    """
      <idea-plugin package="com.intellij.platform.ide.newUiOnboarding">
      </idea-plugin>    
    """
  }

  private val intellijProfilerAsyncOne = Xml("lib/modules.jar", "intellij.profiler.asyncOne") {
    //language=XML
    """
      <idea-plugin package="one.profiler">
      </idea-plugin>    
    """
  }

  private val intellijProfilerUltimateIdeaAsyncProfiler = Xml("lib/modules.jar", "intellij.profiler.ultimate.ideaAsyncProfiler") {
    //language=XML
    """
    <idea-plugin package="com.intellij.profiler.ultimate.async">
      <dependencies>
        <module name="intellij.profiler.asyncOne"/>
      </dependencies>
    </idea-plugin>    
    """.trimIndent()
  }

  private val intellijProfilerCommon = Xml("lib/modules.jar", "intellij.profiler.common") {
    //language=XML
    """
    <idea-plugin package="com.intellij.profiler">
      <module value="com.intellij.modules.profiler"/>
    </idea-plugin>  
    """.trimIndent()
  }

  private val intellijProfilerUltimate = Xml("lib/modules.jar", "intellij.profiler.ultimate") {
    //language=XML
    """
    <idea-plugin package="com.intellij.profiler.ultimate">
      <dependencies>
        <module name="intellij.profiler.common"/>
        <module name="intellij.profiler.ultimate.ideaAsyncProfiler"/>
        <plugin id="com.intellij.java"/>
      </dependencies>
    </idea-plugin>
    """.trimIndent()
  }

  private val intellijNotebooksVisualization = Xml("lib/modules.jar", "intellij.notebooks.visualization") {
    //language=XML
    """
    <idea-plugin package="org.jetbrains.plugins.notebooks.visualization">
      <module value="com.intellij.modules.notebooks.visualization" />
      <dependencies>
        <plugin id="com.intellij.platform.images"/>
        <module name="intellij.notebooks.ui"/>
      </dependencies>
    </idea-plugin>      
    """
  }

  private val intellijNotebooksUi = Xml("lib/modules.jar", "intellij.notebooks.ui") {
    """
    <idea-plugin package="org.jetbrains.plugins.notebooks.ui" />
    """
  }

  private val intellijPlatformImagesCopyright = Xml("lib/modules.jar", "intellij.platform.images.copyright") {
    """
    <idea-plugin package="org.intellij.images.copyright">
      <dependencies>
        <plugin id="com.intellij.platform.images"/>
        <plugin id="com.intellij.copyright"/>
      </dependencies>
    </idea-plugin>
    """
  }

  private val comIntellijPlatformImages = Xml("plugin/platform-images/lib/platform-images.jar#META-INF", "plugin") {
    """
      <idea-plugin implementation-detail="true" package="org.intellij.images">
        <id>com.intellij.platform.images</id>
        <vendor>JetBrains</vendor>
        <dependencies>
          <plugin id="com.intellij.modules.lang"/>
          <plugin id="com.intellij.modules.xml"/>
        </dependencies>
      </idea-plugin>
    """
  }

  private val comIntellijCopyright = Xml("plugins/copyright/lib/copyright.jar", "plugin") {
    //language=XML
    """
    <idea-plugin>
        <id>com.intellij.copyright</id>
        <name>Copyright</name>
        <category>Other Tools</category>
        <vendor>JetBrains</vendor>
    
        <depends>com.intellij.modules.lang</depends>
        <depends>com.intellij.modules.xml</depends>
    </idea-plugin>
    """
  }

  private val intellijXmlBeans = Xml("lib/modules.jar", "intellij.xml.xmlbeans") {
    //language=XML
    """
    <idea-plugin package="com.intellij.xml.tools">
        <dependencies>
            <plugin id="com.intellij.modules.xml"/>
        </dependencies>
    </idea-plugin>
    """
  }

  private val ultimate = Xml("lib/product.jar#META-INF", "ultimate") {
    //language=XML
    """
    <idea-plugin>
      <module value="com.intellij.modules.ultimate"/>
    </idea-plugin>
    """
  }

  private val microservices = Xml("lib/product.jar#META-INF", "microservices") {
    //language=XML
    """
    <idea-plugin>
      <module value="com.intellij.modules.microservices"/>
      <depends>com.intellij.modules.ultimate</depends>
    </idea-plugin>
    """
  }

  private val duplicateDetector = Xml("lib/app.jar#META-INF", "DuplicateDetector") {
    //language=XML
    """
    <idea-plugin>
      <module value="com.intellij.modules.duplicatesDetector"/>
    </idea-plugin>
    """
  }

  private val sshAttach = Xml("lib/product.jar#META-INF", "ssh-attach") {
    //language=XML
    """
    <idea-plugin>
      <module value="com.intellij.modules.ssh.attach"/>
    </idea-plugin>
    """
  }

  private val intellijPlatformSsh = Xml("lib/app-client.jar", "intellij.platform.ssh") {
    //language=XML
    """
    <!--suppress ALL -->
    <idea-plugin xmlns:xi="http://www.w3.org/2001/XInclude">
      <module value="com.intellij.modules.ssh"/>
      <xi:include href="intellij.platform.ssh.ui.xml">
        <xi:fallback/>
      </xi:include>
    </idea-plugin>
    """
  }

  private val intellijPlatformSshUi = Xml("lib/app-client.jar", "intellij.platform.ssh.ui") {
    //language=XML
    """
    <idea-plugin>
      <module value="com.intellij.modules.ssh.ui"/>
    </idea-plugin>
    """
  }

  private val intellijExecutionProcessElevation = Xml("lib/modules.jar", "intellij.execution.process.elevation") {
    //language=XML
    """
    <idea-plugin package="com.intellij.execution.process.elevation">
      <dependencies>
        <module name="intellij.execution.process.mediator.client"/>
        <module name="intellij.execution.process.mediator.common"/>
      </dependencies>
    </idea-plugin>
    """
  }

  private val intellijExecutionProcessClient = Xml("lib/modules.jar", "intellij.execution.process.client") {
    //language=XML
    """
    <idea-plugin package="com.intellij.execution.process.mediator.client">
      <dependencies>
        <module name="intellij.execution.process.mediator.common"/>
        <module name="intellij.execution.process.mediator.daemon"/> <!-- for launcher and in-process server -->
      </dependencies>
    </idea-plugin>
    """
  }

  private val intellijExecutionProcessCommon = Xml("lib/modules.jar", "intellij.execution.process.common") {
    //language=XML
    """
    <idea-plugin package="com.intellij.execution.process.mediator.common"/>
    """
  }

  private val intellijExecutionProcessDaemon = Xml("lib/modules.jar", "intellij.execution.process.daemon") {
    //language=XML
    """
    <idea-plugin package="com.intellij.execution.process.mediator.daemon">
      <dependencies>
        <module name="intellij.execution.process.mediator.common"/>
      </dependencies>
    </idea-plugin>
    """
  }

  private val structuralSearch = Xml("lib/app.jar#META-INF", "structuralsearch") {
    //language=XML
    """
    <idea-plugin package="com.intellij.structuralsearch">
      <module value="com.intellij.modules.structuralsearch"/>
    </idea-plugin>
    """
  }

  private val intellijJson = Xml("lib/app.jar", "intellij.json") {
    //language=XML
    """
    <idea-plugin>
        <module value="com.intellij.modules.json"/>
    </idea-plugin>
    """
  }

  private val xmlPlugin = Xml("lib/app-client.jar#META-INF", "XmlPlugin") {
    //language=XML
    """
    <idea-plugin>
        <module value="com.intellij.modules.xml"/>
    </idea-plugin>
    """.trimIndent()
  }

  private val javaIdePlugin = Xml("lib/app.jar#META-INF", "JavaIdePlugin") {
    //language=XML
    """
    <!--suppress ALL -->
    <idea-plugin xmlns:xi="http://www.w3.org/2001/XInclude">
        <xi:include href="/META-INF/PlatformLangPlugin.xml"/>
        <module value="com.intellij.modules.all"/>
        <module value="com.intellij.modules.jsp.base"/>

        <xi:include href="intellij.platform.remoteServers.impl.xml">
            <xi:fallback/>
        </xi:include>
            <!-- -->        
        <xi:include href="/META-INF/DesignerCorePlugin.xml">
            <xi:fallback/>
        </xi:include>

        <xi:include href="/META-INF/XmlPlugin.xml"/>

        <xi:include href="intellij.json.xml">
            <xi:fallback/>
        </xi:include>

        <xi:include href="/META-INF/structuralsearch.xml">
            <xi:fallback/>
        </xi:include>
    </idea-plugin>
    """
  }

  private val designerCorePlugin = Xml("plugins/java/lib/java-impl.jar#META-INF", "DesignerCorePlugin") {
    //language=XML
    """
    <idea-plugin>
      <module value="com.intellij.ui-designer-new"/>
      <depends>com.intellij.modules.lang</depends>
    </idea-plugin>
    """.trimIndent()
  }

  private val comJetbrainsPerformancePlugin = Xml("plugins/performanceTesting/lib/performanceTesting.jar#META-INF", "plugin") {
    //language=XML
    """
    <idea-plugin package="com.jetbrains.performancePlugin">
        <id>com.jetbrains.performancePlugin</id>
        <name>Performance Testing</name>
        <vendor>JetBrains</vendor>
        <content>
            <module name="intellij.performanceTesting.remoteDriver"><![CDATA[
                <idea-plugin package="com.jetbrains.performancePlugin.remotedriver"></idea-plugin>]]></module>
            <module name="intellij.performanceTesting.vcs"><![CDATA[
                <idea-plugin package="com.intellij.performanceTesting.vcs">
                    <dependencies>
                        <module name="intellij.platform.vcs.impl" />
                        <module name="intellij.platform.vcs.log.impl" />
                    </dependencies>
                </idea-plugin>]]>
            </module>
        </content>
        <dependencies>
            <plugin id="com.intellij.modules.lang"/>
        </dependencies>
    </idea-plugin>      
    """
  }

  private val intellijPlatformRemoteServersImpl = Xml("lib/app-client.jar", "intellij.platform.remoteServers.impl") {
    //language=XML
    """
    <idea-plugin>
        <module value="com.intellij.modules.remoteServers"/>
    </idea-plugin>
    """.trimIndent()
  }

  @Suppress("TestFunctionName")
  private fun Xml(directory: String, name: String, xml: () -> String) = Descriptor(directory, "$name.xml", xml().trimIndent())

  internal data class Descriptor(val directory: String, val name: String, val xml: String)

  @Test
  fun test241() {
    val ide = IdeManager.createManager().createIde(buildIdeaDirectory())
    with(ide.bundledPlugins) {
      assertEquals(5, size)
      assertTrue(any { it.pluginId == "com.intellij" })
      assertTrue(any { it.pluginId == "Git4Idea" })
      assertTrue(any { it.pluginId == "com.intellij.platform.images" })
      assertTrue(any { it.pluginId == "com.intellij.copyright" })
      assertTrue(any { it.pluginId == "com.jetbrains.performancePlugin" })
    }

    val git4Idea = ide.findPluginById("Git4Idea") ?: return

    val dependencyTree = DependencyTree(ide)
    with(dependencyTree.getTransitiveDependencies(git4Idea)) {
      assertEquals(3, size)
      assertContains("com.intellij.modules.vcs")
      assertContains("com.jetbrains.performancePlugin")
      assertContains("com.intellij.modules.lang")
    }
  }

  @Test
  fun `IntelliJ IDEA Community Edition 2024-2 is tested`() {
    val ideResourceLocation = "/ide-dumps/IC-242.24807.4"
    val ideUrl = DependenciesTest::class.java.getResource(ideResourceLocation)
    assertNotNull("Dumped IDE not found in the resources [$ideResourceLocation]", ideUrl)
    ideUrl!!
    val ideRoot = Paths.get(ideUrl.toURI())

    val ide = ProductInfoBasedIdeManager(MissingLayoutFileMode.SKIP_CLASSPATH)
      .createIde(ideRoot)
    with(ide.bundledPlugins) {
      assertEquals(280, size)
    }

    val git4Idea = ide.findPluginById("Git4Idea")
    assertNotNull("No Git4Idea plugin found in the IDE", git4Idea)
    git4Idea!!

    val dependencyTree = DependencyTree(ide)
    with(dependencyTree.getTransitiveDependencies(git4Idea)) {
      assertEquals(30, size)
      listOf(
        "com.jetbrains.performancePlugin",
        "com.intellij.modules.lang",
        "kotlin.features-trainer",
        "intellij.java.featuresTrainer",
        "training",
        "intellij.platform.lvcs.impl",
        "intellij.platform.vcs.impl",
        "intellij.libraries.microba",
        "Git4Idea",
        "com.jetbrains.performancePlugin",
        "intellij.platform.vcs.log.impl",
        "intellij.platform.collaborationTools",
        "intellij.platform.vcs.dvcs.impl",
        "com.intellij.modules.vcs",
        "intellij.platform.ide.newUiOnboarding",
        "org.jetbrains.plugins.terminal",
        "com.jetbrains.sh",
        "com.intellij.copyright",
        "com.intellij.modules.xml",
        "org.jetbrains.kotlin",
        "com.intellij.java",
        "com.intellij.platform.images",
        "com.intellij.modules.idea.community",
        "com.intellij.modules.xdebugger",
        "com.intellij.modules.java-capable",
        "intellij.performanceTesting.vcs",
        "org.intellij.plugins.markdown",
        "org.intellij.intelliLang",
        "com.intellij.modules.java",
        "org.jetbrains.plugins.yaml",
        "org.toml.lang",
        "tanvd.grazi",
        "com.intellij.properties",
        "intellij.platform.coverage",
        "intellij.platform.coverage.agent",
        "intellij.platform.ide.newUsersOnboarding",
        "intellij.platform.experiment",
        "intellij.platform.collaborationTools",
        "com.intellij.modules.vcs",
        "intellij.platform.ide.newUiOnboarding",
        "org.jetbrains.plugins.terminal",
        "intellij.platform.coverage",
      ).forEach(::assertContains)
    }
  }

  @Test
  fun test243Dump() {
    val ideResourceLocation = "/ide-dumps/243.12818.47-1"
    val ideUrl = DependenciesTest::class.java.getResource(ideResourceLocation)
    assertNotNull("Dumped IDE not found in the resources [$ideResourceLocation]", ideUrl)
    ideUrl!!
    val ideRoot = Paths.get(ideUrl.toURI())

    val ide = ProductInfoBasedIdeManager(MissingLayoutFileMode.SKIP_CLASSPATH)
      .createIde(ideRoot)
    with(ide.bundledPlugins) {
      assertEquals(504, size)
    }

    val git4Idea = ide.findPluginById("Git4Idea")
    assertNotNull("No Git4Idea plugin found in the IDE", git4Idea)
    git4Idea!!

    val dependencyTree = DependencyTree(ide)
    with(dependencyTree.getTransitiveDependencies(git4Idea)) {
      val expectedDependencies = setOf(
        DependencyEntry(id = "com.jetbrains.performancePlugin"),
        DependencyEntry(id = "com.intellij.modules.lang", ownerId = "com.intellij", transitive = true),
        DependencyEntry(id = "com.intellij.java", transitive = true),
        DependencyEntry(id = "com.intellij.copyright", transitive = true),
        DependencyEntry(id = "com.intellij.platform.images", transitive = true),
        DependencyEntry(id = "com.intellij.modules.vcs", ownerId = "intellij.platform.vcs.impl", transitive = true),
        DependencyEntry(id = "training", transitive = true),
        DependencyEntry(id = "intellij.platform.lvcs.impl", ownerId = "com.intellij", transitive = true),
        DependencyEntry(id = "kotlin.features-trainer", ownerId = "kotlin.features-trainer", transitive = true),
        DependencyEntry(
          id = "intellij.java.featuresTrainer", ownerId = "intellij.java.featuresTrainer", transitive = true
        ),
        DependencyEntry(id = "org.jetbrains.kotlin", transitive = true),
        DependencyEntry(id = "intellij.platform.collaborationTools", ownerId = "com.intellij", transitive = true),
        DependencyEntry(id = "Git4Idea", transitive = true),
        DependencyEntry(id = "com.jetbrains.performancePlugin", transitive = true),
        DependencyEntry(id = "org.jetbrains.plugins.terminal", transitive = true),
        DependencyEntry(id = "com.jetbrains.sh", transitive = true),
        DependencyEntry(id = "org.intellij.plugins.markdown", transitive = true),
        DependencyEntry(id = "org.intellij.intelliLang", transitive = true),
        DependencyEntry(id = "XPathView", transitive = true),
        DependencyEntry(id = "com.intellij.modules.xml", ownerId = "com.intellij", transitive = true),
        DependencyEntry(id = "com.intellij.modules.java", ownerId = "com.intellij.java", transitive = true),
        DependencyEntry(
          id = "intellij.performanceTesting.vcs", ownerId = "com.jetbrains.performancePlugin", transitive = true
        ),
        DependencyEntry(id = "com.intellij.modules.json", transitive = true),
        DependencyEntry(id = "org.jetbrains.plugins.yaml", transitive = true),
        DependencyEntry(id = "org.toml.lang", transitive = true),
        DependencyEntry(id = "tanvd.grazi", transitive = true),
        DependencyEntry(id = "intellij.platform.vcs.impl", ownerId = "com.intellij", transitive = true),
        DependencyEntry(id = "com.intellij.properties", transitive = true),
        DependencyEntry(id = "intellij.platform.collaborationTools", ownerId = "com.intellij"),
        DependencyEntry(id = "com.intellij.modules.vcs", ownerId = "intellij.platform.vcs.impl"),
        DependencyEntry(id = "org.jetbrains.plugins.terminal"),
        // duplicate, because ModuleV2Dependency is actually a plugin.
        DependencyEntry(id = "com.intellij.modules.json", ownerId = "com.intellij.modules.json", transitive = true)
      )

      assertSetsEqual(expectedDependencies, toDependencyEntries())
    }

    val expectedDebugString = """
      * Plugin dependency: 'com.jetbrains.performancePlugin'
        * Module 'com.intellij.modules.lang' provided by plugin 'com.intellij'
          * Plugin dependency: 'com.intellij.java'
            * Plugin dependency: 'com.intellij.copyright'
              * Module 'com.intellij.modules.lang' provided by plugin 'com.intellij' (already visited)
            * Plugin dependency: 'com.intellij.platform.images'
              * Module 'com.intellij.modules.lang' provided by plugin 'com.intellij' (already visited)
            * Module 'com.intellij.modules.lang' provided by plugin 'com.intellij' (already visited)
            * Module 'com.intellij.modules.vcs' provided by plugin 'intellij.platform.vcs.impl'
            * Plugin dependency: 'training'
              * Module 'intellij.platform.lvcs.impl' provided by plugin 'com.intellij' (already visited)
              * Plugin dependency: 'Git4Idea'
                * Plugin dependency: 'com.jetbrains.performancePlugin' (already visited)
                * Module 'intellij.platform.collaborationTools' provided by plugin 'com.intellij' (already visited)
                * Module 'com.intellij.modules.vcs' provided by plugin 'intellij.platform.vcs.impl' (already visited)
                * Plugin dependency: 'org.jetbrains.plugins.terminal'
                  * Module 'com.intellij.modules.lang' provided by plugin 'com.intellij' (already visited)
                  * Plugin dependency: 'com.jetbrains.sh'
                    * Module 'com.intellij.modules.lang' provided by plugin 'com.intellij' (already visited)
                    * Plugin dependency: 'org.jetbrains.plugins.terminal' (already visited)
                    * Plugin dependency: 'com.intellij.copyright' (already visited)
                    * Plugin dependency: 'org.intellij.plugins.markdown'
                      * Module 'com.intellij.modules.lang' provided by plugin 'com.intellij' (already visited)
                      * Plugin dependency: 'org.intellij.intelliLang'
                        * Plugin dependency: 'XPathView'
                          * Module 'com.intellij.modules.xml' provided by plugin 'com.intellij' (already visited)
                        * Module 'com.intellij.modules.java' provided by plugin 'com.intellij.java' (already visited)
                        * Module 'com.intellij.modules.xml' provided by plugin 'com.intellij' (already visited)
                      * Plugin dependency: 'com.intellij.modules.json'
                      * Plugin dependency: 'org.jetbrains.plugins.yaml'
                        * Module 'com.intellij.modules.lang' provided by plugin 'com.intellij' (already visited)
                        * Module 'com.intellij.modules.json' provided by plugin 'com.intellij.modules.json' (already visited)
                      * Plugin dependency: 'org.toml.lang'
                        * Module 'com.intellij.modules.lang' provided by plugin 'com.intellij' (already visited)
                        * Plugin dependency: 'com.intellij.modules.json' (already visited)
                        * Plugin dependency: 'tanvd.grazi'
                          * Module 'intellij.platform.vcs.impl' provided by plugin 'com.intellij' (already visited)
                          * Plugin dependency: 'com.intellij.java' (already visited)
                          * Plugin dependency: 'com.intellij.modules.json' (already visited)
                          * Plugin dependency: 'org.intellij.plugins.markdown' (already visited)
                          * Plugin dependency: 'com.intellij.properties'
                            * Module 'com.intellij.modules.xml' provided by plugin 'com.intellij' (already visited)
                            * Plugin dependency: 'com.intellij.copyright' (already visited)
                          * Plugin dependency: 'org.jetbrains.plugins.yaml' (already visited)
                      * Plugin dependency: 'com.intellij.platform.images' (already visited)
            * Module 'intellij.performanceTesting.vcs' provided by plugin 'com.jetbrains.performancePlugin' (already visited)
          * Module 'kotlin.features-trainer' provided by plugin 'kotlin.features-trainer'
            * Module 'intellij.java.featuresTrainer' provided by plugin 'intellij.java.featuresTrainer'
              * Plugin dependency: 'training' (already visited)
            * Plugin dependency: 'training' (already visited)
          * Module 'intellij.java.featuresTrainer' provided by plugin 'intellij.java.featuresTrainer' (already visited)
          * Plugin dependency: 'org.jetbrains.kotlin'
            * Module 'intellij.platform.collaborationTools' provided by plugin 'com.intellij' (already visited)
            * Plugin dependency: 'com.intellij.java' (already visited)
          * Plugin dependency: 'com.intellij.platform.images' (already visited)
          * Plugin dependency: 'com.intellij.copyright' (already visited)
      * Module 'intellij.platform.collaborationTools' provided by plugin 'com.intellij' (already visited)
      * Module 'com.intellij.modules.vcs' provided by plugin 'intellij.platform.vcs.impl' (already visited)
      * Plugin dependency: 'org.jetbrains.plugins.terminal' (already visited)

    """.trimIndent()

    assertEquals(expectedDebugString, dependencyTree.toDebugString(git4Idea.pluginId!!).toString())
  }

  @Test
  fun `coverage plugin is resolved`() {
    val ideResourceLocation = "/ide-dumps/243.12818.47-1"
    val ideUrl = DependenciesTest::class.java.getResource(ideResourceLocation)
    assertNotNull("Dumped IDE not found in the resources [$ideResourceLocation]", ideUrl)
    ideUrl!!
    val ideRoot = Paths.get(ideUrl.toURI())

    val ide = ProductInfoBasedIdeManager(MissingLayoutFileMode.SKIP_CLASSPATH)
      .createIde(ideRoot)

    val coveragePlugin = ide.findPluginById("Coverage")
    assertNotNull("No 'Coverage' plugin found in the IDE", coveragePlugin)
    coveragePlugin!!

    val dependencyTree = DependencyTree(ide)
    with(dependencyTree.getTransitiveDependencies(coveragePlugin)) {
      assertSetsEqual(expectedCoveragePluginDependencies, toDependencyEntries())
    }
  }

  @Test
  fun `coverage plugin has correct transitive classpath`() {
    val ideResourceLocation = "/ide-dumps/243.12818.47-1"
    val ideUrl = DependenciesTest::class.java.getResource(ideResourceLocation)
    assertNotNull("Dumped IDE not found in the resources [$ideResourceLocation]", ideUrl)
    ideUrl!!
    val ideRoot = Paths.get(ideUrl.toURI())

    val ide = ProductInfoBasedIdeManager(MissingLayoutFileMode.SKIP_CLASSPATH)
      .createIde(ideRoot)

    val coveragePlugin = ide.findPluginById("Coverage")
    assertNotNull("No 'Coverage' plugin found in the IDE", coveragePlugin)
    coveragePlugin!!

    val dependencyTree = DependencyTree(ide)
    val dependencies = dependencyTree.getTransitiveDependencies(coveragePlugin)
    val transitiveClasspath = dependencies.flatMap {
      when (it) {
        is Dependency.Module -> it.plugin.classpath.paths
        is Dependency.Plugin -> it.plugin.classpath.paths
        else -> emptyList()
      }
    }
    val classpath = coveragePlugin.classpath.paths + transitiveClasspath

    val relativeClasspaths = classpath
      .map { ideRoot.relativize(it) }
      .map { it.toString() }
      .toSet()

    val expectedClassPath = """
      plugins/java-coverage/lib/java-coverage.jar
      plugins/testng/lib/testng-plugin.jar
      plugins/java/lib/java-impl.jar
      plugins/java/lib/java-frontback.jar
      plugins/java/lib/modules/intellij.java.unscramble.jar
      plugins/java/lib/modules/intellij.java.featuresTrainer.jar
      plugins/java/lib/modules/intellij.java.vcs.jar
      plugins/java/lib/modules/intellij.java.structuralSearch.jar
      plugins/copyright/lib/copyright.jar
      lib/product.jar
      lib/testFramework.jar
      lib/idea_rt.jar
      lib/app-client.jar
      lib/modules/intellij.platform.lvcs.impl.jar
      lib/modules/intellij.profiler.common.jar
      lib/modules/intellij.platform.dap.jar
      lib/modules/intellij.platform.ide.newUsersOnboarding.jar
      lib/modules/intellij.platform.images.copyright.jar
      lib/modules/intellij.ide.startup.importSettings.jar
      lib/modules/intellij.execution.process.elevation.jar
      lib/modules/intellij.platform.clouds.jar
      lib/modules/intellij.platform.settings.local.jar
      lib/modules/intellij.kotlin.onboarding-promoter.jar
      lib/modules/intellij.platform.navbar.monolith.jar
      lib/modules/intellij.platform.execution.dashboard.jar
      lib/modules/intellij.profiler.ultimate.jar
      lib/modules/intellij.platform.coverage.jar
      lib/modules/intellij.platform.coverage.agent.jar
      lib/modules/intellij.xml.xmlbeans.jar
      lib/modules/intellij.platform.navbar.jar
      lib/modules/intellij.profiler.ultimate.ideaAsyncProfiler.jar
      lib/modules/intellij.platform.vcs.dvcs.impl.jar
      lib/modules/intellij.platform.ml.embeddings.jar
      lib/modules/intellij.execution.process.mediator.daemon.jar
      lib/modules/intellij.platform.images.backend.svg.jar
      lib/modules/intellij.platform.navbar.backend.jar
      lib/modules/intellij.profiler.asyncOne.jar
      lib/modules/intellij.platform.execution.serviceView.jar
      lib/modules/intellij.libraries.ktor.client.cio.jar
      lib/modules/intellij.platform.compose.jar
      lib/modules/intellij.platform.collaborationTools.jar
      lib/modules/intellij.smart.update.jar
      lib/modules/intellij.libraries.skiko.jar
      lib/modules/intellij.libraries.grpc.jar
      lib/modules/intellij.libraries.compose.desktop.jar
      lib/modules/intellij.libraries.grpc.netty.shaded.jar
      lib/modules/intellij.platform.rpc.backend.jar
      lib/modules/intellij.libraries.ktor.client.jar
      lib/modules/intellij.execution.process.mediator.client.jar
      lib/modules/intellij.platform.vcs.log.impl.jar
      lib/modules/intellij.platform.smRunner.vcs.jar
      lib/modules/intellij.platform.experiment.jar
      lib/modules/intellij.platform.navbar.frontend.jar
      lib/modules/intellij.platform.vcs.impl.jar
      lib/modules/intellij.idea.customization.base.jar
      lib/modules/intellij.platform.kernel.backend.jar
      lib/modules/intellij.platform.ide.newUiOnboarding.jar
      lib/modules/intellij.libraries.microba.jar
      lib/modules/intellij.execution.process.mediator.common.jar
      plugins/platform-images/lib/platform-images.jar
      plugins/featuresTrainer/lib/featuresTrainer.jar
      plugins/vcs-git/lib/vcs-git.jar
      plugins/performanceTesting/lib/performanceTesting.jar
      plugins/terminal/lib/terminal.jar
      plugins/sh/lib/sh.jar
      plugins/markdown/lib/markdown.jar
      plugins/markdown/lib/modules/intellij.markdown.compose.preview.jar
      plugins/platform-langInjection/lib/platform-langInjection.jar
      plugins/xpath/lib/xpath.jar
      plugins/Kotlin/lib/kotlin-plugin-shared.jar
      plugins/Kotlin/lib/kotlin-plugin.jar
      plugins/json/lib/json.jar
      plugins/yaml/lib/yaml-editing.jar
      plugins/yaml/lib/yaml.jar
      plugins/toml/lib/toml.jar
      plugins/grazie/lib/grazie.jar
      plugins/properties/lib/properties.jar
      plugins/junit/lib/junit.jar
    """.trimIndent().split("\\s".toRegex()).toSet()

    assertEquals(expectedClassPath, relativeClasspaths.map { it.toString() }.toSet())
  }

  @Test
  fun `plugin depends on an content module, but the content module owner is resolved as a single dependency`() {
    val plugin = buildPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
              <content>
                <module name="intellij.libraries.microba"><![CDATA[<idea-plugin />]]></module>
              </content>
              <content>
                <module name="intellij.platform.vcs.impl"><![CDATA[
                  <idea-plugin>
                    <module value="com.intellij.modules.vcs" />
                    <dependencies>
                      <module name="intellij.libraries.microba" />
                    </dependencies>                
                  </idea-plugin>]]>
                </module>
              </content>
            </idea-plugin>
          """
        }
      }
    }
    val dependantPlugin = buildPlugin("dependant-plugin.jar") {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              <id>dependantPlugin</id>
              <name>Dependant</name>
              <version>someVersion</version>
              <vendor email="vendor.com" url="url">vendor</vendor>
              <description>this description is looooooooooong enough</description>
              <idea-version since-build="131.1"/>
              <!-- depends on a content module -->                            
              <depends>intellij.platform.vcs.impl</depends>
            </idea-plugin>
          """
        }
      }
    }

    val pluginProvider = EventLogSinglePluginProvider(plugin)

    val dependencyTree = DependencyTree(pluginProvider)
    with(dependencyTree.getTransitiveDependencies(dependantPlugin)) {
      assertEquals(1, size)
      assertEquals(Dependency.Module(plugin, "intellij.platform.vcs.impl"), single())
    }

    with(pluginProvider.pluginSearchLog) {
      assertEquals(1, size)
      assertEquals(LogEntry("intellij.platform.vcs.impl", plugin, "found via content module ID"), this[0])
    }
  }

  @Test
  fun `plugin depends on an IDE module`() {
    val ideModule = buildPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
              <module value="intellij.java.terminal" />
            </idea-plugin>
          """
        }
      }
    }
    val dependantPlugin = buildPlugin("dependant-plugin.jar") {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              <id>dependantPlugin</id>
              <name>Dependant</name>
              <version>someVersion</version>
              <vendor email="vendor.com" url="url">vendor</vendor>
              <description>this description is looooooooooong enough</description>
              <idea-version since-build="131.1"/>
              <!-- depends on a plugin alias -->                            
              <depends>intellij.java.terminal</depends>
            </idea-plugin>
          """
        }
      }
    }

    val ideModulePredicate = DefaultIdeModulePredicate(setOf("intellij.java.terminal"))
    val pluginProvider = EventLogSinglePluginProvider(ideModule)

    val dependencyTree = DependencyTree(pluginProvider, ideModulePredicate)
    with(dependencyTree.getTransitiveDependencies(dependantPlugin)) {
      assertEquals(1, size)
      assertEquals(Dependency.Module(ideModule, "intellij.java.terminal"), single())
    }

    with(pluginProvider.pluginSearchLog) {
      assertEquals(1, size)
      assertEquals(LogEntry("intellij.java.terminal", ideModule, "found via plugin alias"), this[0])
    }
  }

  @Test
  fun `plugin depends on a plugin alias in another plugin`() {
    val ideModule = buildPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
              <module value="intellij.java.terminal" />
            </idea-plugin>
          """
        }
      }
    }
    val dependantPlugin = buildPlugin("dependant-plugin.jar") {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              <id>dependantPlugin</id>
              <name>Dependant</name>
              <version>someVersion</version>
              <vendor email="vendor.com" url="url">vendor</vendor>
              <description>this description is looooooooooong enough</description>
              <idea-version since-build="131.1"/>
              <!-- depends on a plugin alias -->                            
              <depends>intellij.java.terminal</depends>
            </idea-plugin>
          """
        }
      }
    }

    val pluginProvider = EventLogSinglePluginProvider(ideModule)

    val dependencyTree = DependencyTree(pluginProvider)
    with(dependencyTree.getTransitiveDependencies(dependantPlugin)) {
      assertEquals(1, size)
      assertEquals(Dependency.Plugin(ideModule), single())
    }

    with(pluginProvider.pluginSearchLog) {
      assertEquals(1, size)
      assertEquals(LogEntry("intellij.java.terminal", ideModule, "found via plugin alias"), this[0])
    }
  }

  @Test
  fun `plugin depends via v1 on an IDE module`() {
    val plugin = buildPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
              <module value="com.intellij.modules.java" />
            </idea-plugin>
          """
        }
      }
    }
    val dependantPlugin = buildPlugin("dependant-plugin.jar") {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              <id>dependantPlugin</id>
              <name>Dependant</name>
              <version>someVersion</version>
              <vendor email="vendor.com" url="url">vendor</vendor>
              <description>this description is looooooooooong enough</description>
              <idea-version since-build="131.1"/>
              <!-- depends on an IDE module -->                            
              <depends>com.intellij.modules.java</depends>
            </idea-plugin>
          """
        }
      }
    }

    val pluginProvider = EventLogSinglePluginProvider(plugin)

    val dependencyTree = DependencyTree(pluginProvider)
    with(dependencyTree.getTransitiveDependencies(dependantPlugin)) {
      assertEquals(1, size)
      assertEquals(Dependency.Module(plugin, "com.intellij.modules.java"), single())
    }

    with(pluginProvider.pluginSearchLog) {
      assertEquals(1, size)
      assertEquals(LogEntry("com.intellij.modules.java", plugin, "found via plugin alias"), this[0])
    }
  }

  @Test
  fun `dependency tree uses a missing dependency in multiple places`() {
    val plugin = buildPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
              <module value="com.intellij.modules.java" />
              <depends>nonexistent.plugin</depends>
            </idea-plugin>
          """
        }
      }
    }
    val dependantPlugin = buildPlugin("dependant-plugin.jar") {
      dir("META-INF") {
        file("plugin.xml") {
          perfectXmlBuilder.modify {
            id = "<id>dependantPlugin</id>"
            name = "<name>Dependant</name>"
            depends = "<depends>com.intellij.modules.java</depends>" +
                      "<depends>nonexistent.plugin</depends>"
          }
        }
      }
    }
    val pluginProvider = EventLogSinglePluginProvider(plugin)

    val dependencyTree = DependencyTree(pluginProvider)
    with(dependencyTree.getTransitiveDependencies(dependantPlugin)) {
      assertEquals(1, size)
      assertEquals(Dependency.Module(plugin, "com.intellij.modules.java"), single())
    }

    with(pluginProvider.pluginSearchLog) {
      assertEquals(2, size)
      with(this[0]) {
        assertEquals("com.intellij.modules.java", pluginId)
        assertEquals("found via plugin alias", reason)
      }
      with(this[1]) {
        assertEquals("nonexistent.plugin", pluginId)
        assertEquals("not found", reason)
      }
    }
  }

  private val expectedCoveragePluginDependencies = setOf(
    DependencyEntry(id = "Git4Idea", transitive = true),
    DependencyEntry(id = "JUnit", transitive = false),
    DependencyEntry(id = "TestNG-J", transitive = false),
    DependencyEntry(id = "XPathView", transitive = true),
    DependencyEntry(id = "com.intellij.copyright", transitive = true),
    DependencyEntry(id = "com.intellij.java", transitive = false),
    DependencyEntry(id = "com.intellij.java", transitive = true),
    DependencyEntry(id = "com.intellij.modules.java", ownerId = "com.intellij.java", transitive = true),
    DependencyEntry(id = "com.intellij.modules.json", ownerId = "com.intellij.modules.json", transitive = true),
    // duplicate, because ModuleV2Dependency is actually a plugin.
    DependencyEntry(id = "com.intellij.modules.json", ownerId = null, transitive = true),
    DependencyEntry(id = "com.intellij.modules.lang", ownerId = "com.intellij", transitive = true),
    DependencyEntry(id = "com.intellij.modules.vcs", ownerId = "intellij.platform.vcs.impl", transitive = true),
    DependencyEntry(id = "com.intellij.modules.xml", ownerId = "com.intellij", transitive = true),
    DependencyEntry(id = "com.intellij.platform.images", transitive = true),
    DependencyEntry(id = "com.intellij.properties", transitive = true),
    DependencyEntry(id = "com.jetbrains.performancePlugin", transitive = true),
    DependencyEntry(id = "com.jetbrains.sh", transitive = true),
    DependencyEntry(id = "intellij.java.featuresTrainer", ownerId = "intellij.java.featuresTrainer", transitive = true),
    DependencyEntry(id = "intellij.performanceTesting.vcs", ownerId = "com.jetbrains.performancePlugin", transitive = true),
    DependencyEntry(id = "intellij.platform.collaborationTools", ownerId = "com.intellij", transitive = true),
    DependencyEntry(id = "intellij.platform.coverage", ownerId = "com.intellij", transitive = false),
    DependencyEntry(id = "intellij.platform.lvcs.impl", ownerId = "com.intellij", transitive = true),
    DependencyEntry(id = "intellij.platform.vcs.impl", ownerId = "com.intellij", transitive = true),
    DependencyEntry(id = "kotlin.features-trainer", ownerId = "kotlin.features-trainer", transitive = true),
    DependencyEntry(id = "org.intellij.intelliLang", transitive = true),
    DependencyEntry(id = "org.intellij.plugins.markdown", transitive = true),
    DependencyEntry(id = "org.jetbrains.kotlin", transitive = true),
    DependencyEntry(id = "org.jetbrains.plugins.terminal", transitive = true),
    DependencyEntry(id = "org.jetbrains.plugins.yaml", transitive = true),
    DependencyEntry(id = "org.toml.lang", transitive = true),
    DependencyEntry(id = "tanvd.grazi", transitive = true),
    DependencyEntry(id = "training", transitive = true),
  )

  private val expectedCoveragePluginDependencyIdentifiers = listOf(
    "Git4Idea",
    "JUnit",
    "TestNG-J",
    "XPathView",
    "com.intellij.copyright",
    "com.intellij.java",
    "com.intellij.java", // duplicate via transitive dependency
    "com.intellij.modules.java provided by plugin com.intellij.java",
    "com.intellij.modules.json",
    "com.intellij.modules.lang provided by plugin com.intellij",
    "com.intellij.modules.vcs provided by plugin intellij.platform.vcs.impl",
    "com.intellij.modules.xml provided by plugin com.intellij",
    "com.intellij.platform.images",
    "com.intellij.properties",
    "com.jetbrains.performancePlugin",
    "com.jetbrains.sh",
    "intellij.java.featuresTrainer provided by plugin intellij.java.featuresTrainer",
    "intellij.performanceTesting.vcs provided by plugin com.jetbrains.performancePlugin",
    "intellij.platform.collaborationTools provided by plugin com.intellij",
    "intellij.platform.coverage provided by plugin com.intellij",
    "intellij.platform.lvcs.impl provided by plugin com.intellij",
    "intellij.platform.vcs.impl provided by plugin com.intellij",
    "kotlin.features-trainer provided by plugin kotlin.features-trainer",
    "org.intellij.intelliLang",
    "org.intellij.plugins.markdown",
    "org.jetbrains.kotlin",
    "org.jetbrains.plugins.terminal",
    "org.jetbrains.plugins.yaml",
    "org.toml.lang",
    "tanvd.grazi",
    "training"
  )

  private fun buildPlugin(pluginName: String = "plugin.jar", pluginContentBuilder: ContentBuilder.() -> Unit): IdePlugin {
    val pluginFile = buildZipFile(temporaryFolder.newFile(pluginName).toPath(), pluginContentBuilder)
    val pluginCreationResult = IdePluginManager.createManager().createPlugin(pluginFile)
    if (pluginCreationResult is PluginCreationFail) {
      fail(pluginCreationResult.errorsAndWarnings.joinToString { it.message })
    }
    return (pluginCreationResult as PluginCreationSuccess).plugin
  }

  private fun Collection<Dependency>.toDependencyEntries(): Set<DependencyEntry> {
    return mapNotNull {
      when (it) {
        is Dependency.Plugin -> DependencyEntry(it.plugin.id, transitive = it.isTransitive)
        is Dependency.Module -> DependencyEntry(it.id, it.plugin.id, transitive = it.isTransitive)
        Dependency.None -> null
      }
    }.toSet()
  }
}

private data class DependencyEntry(val id: String, val ownerId: String? = null, val transitive: Boolean = false)

private fun Set<Dependency>.assertContains(id: String): Boolean =
  filterIsInstance<PluginAware>()
    .any { it.plugin.pluginId == id }

fun <T> assertSetsEqual(expected: Set<T>, actual: Set<T>) {
  val missing = expected - actual
  val extra = actual - expected

  if (missing.isNotEmpty() || extra.isNotEmpty()) {
    val message = buildString {
      appendLine("Sets are not equal.")
      if (missing.isNotEmpty()) {
        appendLine("Missing elements: $missing")
      }
      if (extra.isNotEmpty()) {
        appendLine("Extra elements: $extra")
      }
    }
    fail(message)
  }
}
