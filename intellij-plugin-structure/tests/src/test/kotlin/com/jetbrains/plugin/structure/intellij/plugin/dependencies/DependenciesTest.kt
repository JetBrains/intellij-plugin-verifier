package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.ide.ProductInfoBasedIdeManager
import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path
import java.nio.file.Paths

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
      assertEquals(42, size)
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
      assertEquals(46, size)
      listOf(
        "XPathView",
        "com.intellij.copyright",
        "com.intellij.java",
        "com.intellij.modules.java",
        "com.intellij.modules.java-capable",
        "com.intellij.modules.json",
        "com.intellij.modules.lang",
        "com.intellij.modules.vcs",
        "com.intellij.modules.xdebugger",
        "com.intellij.modules.xml",
        "com.intellij.platform.images",
        "com.intellij.properties",
        "com.jetbrains.performancePlugin",
        "com.jetbrains.performancePlugin",
        "com.jetbrains.sh",
        "intellij.libraries.microba",
        "intellij.performanceTesting.vcs",
        "intellij.platform.collaborationTools",
        "intellij.platform.coverage",
        "intellij.platform.coverage.agent",
        "intellij.platform.ide.newUiOnboarding",
        "intellij.platform.vcs.dvcs.impl",
        "intellij.platform.vcs.impl",
        "intellij.platform.vcs.log.impl",
        "org.intellij.intelliLang",
        "org.intellij.plugins.markdown",
        "org.jetbrains.plugins.terminal",
        "org.jetbrains.plugins.terminal",
        "org.jetbrains.plugins.yaml",
        "org.toml.lang",
        "tanvd.grazi",
      ).forEach(::assertContains)
    }

    val expectedDebugString = """
      * Plugin dependency: 'com.jetbrains.performancePlugin'
        * Module 'com.intellij.modules.lang' provided by plugin 'com.intellij'
          * Plugin dependency: 'com.intellij.java'
            * Plugin dependency: 'com.intellij.copyright'
              * Module 'com.intellij.modules.lang' provided by plugin 'com.intellij' (already visited)
              * Module 'com.intellij.modules.xml' provided by plugin 'com.intellij' (already visited)
              * Module 'intellij.platform.vcs.impl' provided by plugin 'intellij.platform.vcs.impl'
                * Module 'intellij.libraries.microba' provided by plugin 'intellij.libraries.microba'
            * Plugin dependency: 'com.intellij.platform.images'
              * Module 'com.intellij.modules.lang' provided by plugin 'com.intellij' (already visited)
            * Module 'com.intellij.modules.lang' provided by plugin 'com.intellij' (already visited)
            * Module 'com.intellij.modules.vcs' provided by plugin 'intellij.platform.vcs.impl' (already visited)
            * Module 'com.intellij.modules.xdebugger' provided by plugin 'com.intellij' (already visited)
            * Module 'com.intellij.modules.xml' provided by plugin 'com.intellij' (already visited)
            * Module 'com.intellij.modules.java-capable' provided by plugin 'com.intellij' (already visited)
            * Plugin dependency: 'training'
              * Module 'intellij.platform.lvcs.impl' provided by plugin 'intellij.platform.lvcs.impl'
                * Module 'intellij.platform.vcs.impl' provided by plugin 'intellij.platform.vcs.impl' (already visited)
              * Module 'com.intellij.modules.lang' provided by plugin 'com.intellij' (already visited)
              * Plugin dependency: 'Git4Idea'
                * Plugin dependency: 'com.jetbrains.performancePlugin' (already visited)
                * Module 'intellij.platform.collaborationTools' provided by plugin 'intellij.platform.collaborationTools'
                  * Module 'intellij.platform.vcs.dvcs.impl' provided by plugin 'intellij.platform.vcs.dvcs.impl'
                    * Module 'intellij.platform.vcs.log.impl' provided by plugin 'intellij.platform.vcs.log.impl'
                      * Module 'intellij.platform.vcs.impl' provided by plugin 'intellij.platform.vcs.impl' (already visited)
                  * Module 'intellij.platform.vcs.log.impl' provided by plugin 'intellij.platform.vcs.log.impl' (already visited)
                * Module 'com.intellij.modules.vcs' provided by plugin 'intellij.platform.vcs.impl' (already visited)
                * Module 'intellij.platform.ide.newUiOnboarding' provided by plugin 'intellij.platform.ide.newUiOnboarding'
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
                        * Plugin dependency: 'com.intellij.modules.json' (already visited)
                      * Plugin dependency: 'org.toml.lang'
                        * Module 'com.intellij.modules.lang' provided by plugin 'com.intellij' (already visited)
                        * Plugin dependency: 'com.intellij.modules.json' (already visited)
                        * Plugin dependency: 'tanvd.grazi'
                          * Module 'intellij.platform.vcs.impl' provided by plugin 'intellij.platform.vcs.impl' (already visited)
                          * Plugin dependency: 'com.intellij.java' (already visited)
                          * Plugin dependency: 'com.intellij.modules.json' (already visited)
                          * Plugin dependency: 'org.intellij.plugins.markdown' (already visited)
                          * Plugin dependency: 'com.intellij.properties'
                            * Module 'com.intellij.modules.xml' provided by plugin 'com.intellij' (already visited)
                            * Plugin dependency: 'com.intellij.copyright' (already visited)
                          * Module 'com.intellij.modules.xml' provided by plugin 'com.intellij' (already visited)
                          * Plugin dependency: 'org.jetbrains.plugins.yaml' (already visited)
                      * Plugin dependency: 'com.intellij.platform.images' (already visited)
                      * Module 'com.intellij.modules.xml' provided by plugin 'com.intellij' (already visited)
                      * Module 'intellij.platform.compose' provided by plugin 'intellij.platform.compose'
                        * Module 'intellij.libraries.compose.desktop' provided by plugin 'intellij.libraries.compose.desktop'
                          * Module 'intellij.libraries.skiko' provided by plugin 'intellij.libraries.skiko'
                        * Module 'intellij.libraries.skiko' provided by plugin 'intellij.libraries.skiko' (already visited)
                * Module 'intellij.platform.coverage' provided by plugin 'intellij.platform.coverage'
                  * Module 'intellij.platform.coverage.agent' provided by plugin 'intellij.platform.coverage.agent'
              * Module 'intellij.platform.ide.newUiOnboarding' provided by plugin 'intellij.platform.ide.newUiOnboarding' (already visited)
              * Module 'intellij.platform.ide.newUsersOnboarding' provided by plugin 'intellij.platform.ide.newUsersOnboarding'
                * Module 'intellij.platform.ide.newUiOnboarding' provided by plugin 'intellij.platform.ide.newUiOnboarding' (already visited)
                * Module 'intellij.platform.experiment' provided by plugin 'intellij.platform.experiment'
            * Module 'intellij.performanceTesting.vcs' provided by plugin 'intellij.performanceTesting.vcs'
              * Module 'intellij.platform.vcs.impl' provided by plugin 'intellij.platform.vcs.impl' (already visited)
              * Module 'intellij.platform.vcs.log.impl' provided by plugin 'intellij.platform.vcs.log.impl' (already visited)
            * Plugin dependency: 'com.jetbrains.performancePlugin' (already visited)
            * Module 'intellij.platform.vcs.impl' provided by plugin 'intellij.platform.vcs.impl' (already visited)
          * Module 'kotlin.features-trainer' provided by plugin 'kotlin.features-trainer'
            * Module 'intellij.java.featuresTrainer' provided by plugin 'intellij.java.featuresTrainer'
              * Plugin dependency: 'training' (already visited)
            * Plugin dependency: 'training' (already visited)
          * Module 'intellij.java.featuresTrainer' provided by plugin 'intellij.java.featuresTrainer' (already visited)
          * Plugin dependency: 'org.jetbrains.kotlin'
            * Module 'intellij.platform.collaborationTools' provided by plugin 'intellij.platform.collaborationTools' (already visited)
            * Plugin dependency: 'com.intellij.java' (already visited)
          * Plugin dependency: 'com.intellij.platform.images' (already visited)
          * Plugin dependency: 'com.intellij.copyright' (already visited)
        * Module 'intellij.platform.vcs.impl' provided by plugin 'intellij.platform.vcs.impl' (already visited)
        * Module 'intellij.platform.vcs.log.impl' provided by plugin 'intellij.platform.vcs.log.impl' (already visited)
      * Module 'intellij.platform.collaborationTools' provided by plugin 'intellij.platform.collaborationTools' (already visited)
      * Module 'com.intellij.modules.vcs' provided by plugin 'intellij.platform.vcs.impl' (already visited)
      * Module 'intellij.platform.ide.newUiOnboarding' provided by plugin 'intellij.platform.ide.newUiOnboarding' (already visited)
      * Plugin dependency: 'org.jetbrains.plugins.terminal' (already visited)
      * Module 'intellij.platform.coverage' provided by plugin 'intellij.platform.coverage' (already visited)
      
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
      assertEquals(44, size)
      assertContains("intellij.platform.coverage.agent")
    }
  }
}

private fun Set<Dependency>.assertContains(id: String): Boolean =
  filterIsInstance<PluginAware>()
    .any { it.plugin.pluginId == id }
