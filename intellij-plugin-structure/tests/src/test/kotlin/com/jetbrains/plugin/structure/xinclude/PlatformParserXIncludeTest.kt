/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.xinclude

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.Settings
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.problems.ConditionalIncludeNotSupported
import com.jetbrains.plugin.structure.intellij.resources.DefaultResourceResolver
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * `<xi:include>` resolution on the platform-parser path, which resolves includes through the library's
 * own engine driven by
 * [ResourceResolverXIncludeLoader][com.jetbrains.plugin.structure.intellij.xinclude.ResourceResolverXIncludeLoader]
 * rather than through [XIncluder][com.jetbrains.plugin.structure.intellij.xinclude.XIncluder].
 *
 * Every descriptor below declares `until-build="263.*"`, which is what puts it on the platform parser
 * under the selection rule ([com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.shouldUsePlatformParser]).
 *
 * Each include contributes a plugin alias, which is simply the cheapest thing to observe: an alias
 * appears in [IdePlugin.pluginAliases] if and only if the included document was really read and merged.
 */
class PlatformParserXIncludeTest {
  @Before
  fun enablePlatformParser() {
    Settings.PLATFORM_PARSER_ENABLED.set(true.toString())
  }

  @After
  fun disablePlatformParser() {
    Settings.PLATFORM_PARSER_ENABLED.set(false.toString())
  }

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `an include is resolved within the same jar`() {
    val plugin = buildPluginSuccessfully("same-jar") {
      dir("lib") {
        zip("main.jar") {
          dir("META-INF") {
            file("plugin.xml", pluginXml(include("extensions.xml")))
            file("extensions.xml", aliasXml("alias.from.same.jar"))
          }
        }
      }
    }
    assertEquals(setOf("alias.from.same.jar"), plugin.pluginAliases)
  }

  @Test
  fun `an include is resolved from a sibling jar of the lib directory`() {
    /*
    The included descriptor lives in a DIFFERENT jar of the `lib` directory than the descriptor
    referencing it; a loader that only looked inside the artifact holding plugin.xml could not see it.
    This is why includes go through the plugin's composite ResourceResolver chain.
    */
    val plugin = buildPluginSuccessfully("sibling-jar") {
      dir("lib") {
        zip("main.jar") {
          dir("META-INF") {
            file("plugin.xml", pluginXml(include("shared-extensions.xml")))
          }
        }
        zip("shared.jar") {
          dir("META-INF") {
            file("shared-extensions.xml", aliasXml("alias.from.sibling.jar"))
          }
        }
      }
    }
    assertEquals(setOf("alias.from.sibling.jar"), plugin.pluginAliases)
  }

  @Test
  fun `an include is resolved in an exploded directory plugin`() {
    // Here the resource root is a real directory rather than a zip filesystem root.
    val plugin = buildPluginSuccessfully("exploded") {
      dir("META-INF") {
        file("plugin.xml", pluginXml(include("extensions.xml")))
        file("extensions.xml", aliasXml("alias.from.directory"))
      }
    }
    assertEquals(setOf("alias.from.directory"), plugin.pluginAliases)
  }

  @Test
  fun `nested includes are resolved across artifacts`() {
    val plugin = buildPluginSuccessfully("nested") {
      dir("lib") {
        zip("main.jar") {
          dir("META-INF") {
            file("plugin.xml", pluginXml(include("first.xml")))
          }
        }
        zip("second.jar") {
          dir("META-INF") {
            // Resolved out of main.jar's sibling, and itself pulls in a third artifact.
            file("first.xml", ideaPlugin("""<module value="alias.first"/>""" + include("second.xml")))
          }
        }
        zip("third.jar") {
          dir("META-INF") {
            file("second.xml", aliasXml("alias.second"))
          }
        }
      }
    }
    assertEquals(setOf("alias.first", "alias.second"), plugin.pluginAliases)
  }

  @Test
  fun `an absolute include href is resolved from the resource root`() {
    val plugin = buildPluginSuccessfully("absolute-href") {
      dir("lib") {
        zip("main.jar") {
          dir("META-INF") {
            file("plugin.xml", pluginXml(include("/jarRoot.xml")))
          }
          file("jarRoot.xml", aliasXml("alias.from.jar.root"))
        }
      }
    }
    assertEquals(setOf("alias.from.jar.root"), plugin.pluginAliases)
  }

  @Test
  fun `an unresolvable required include fails the plugin rather than leaving a hole in it`() {
    val result = buildPlugin("missing-required") {
      dir("lib") {
        zip("main.jar") {
          dir("META-INF") {
            file("plugin.xml", pluginXml(include("absent.xml")))
          }
        }
      }
    }
    val fail = assertFailed(result)
    assertTrue(
      "expected the unresolved include to be reported, got ${fail.errorsAndWarnings}",
      fail.errorsAndWarnings.any { it.message.contains("absent.xml") }
    )
  }

  @Test
  fun `an unresolvable include marked optional by a fallback does not fail the plugin`() {
    /*
    The library treats `<xi:fallback>` purely as a marker meaning "this include is optional": it
    skips the fallback element without merging its content (XmlReader.readInclude). So the plugin
    survives the missing include, but the fallback body contributes nothing - as with XIncluder,
    which also ignores the fallback's children.
    */
    val plugin = buildPluginSuccessfully("optional-fallback") {
      dir("lib") {
        zip("main.jar") {
          dir("META-INF") {
            file(
              "plugin.xml",
              pluginXml(
                """
                <xi:include xmlns:xi="http://www.w3.org/2001/XInclude" href="absent.xml">
                  <xi:fallback><module value="alias.from.fallback"/></xi:fallback>
                </xi:include>
                """.trimIndent()
              )
            )
          }
        }
      }
    }
    assertEquals(emptySet(), plugin.pluginAliases)
  }

  @Test
  fun `an unresolvable include with an empty fallback does not fail the plugin`() {
    val plugin = buildPluginSuccessfully("empty-fallback") {
      dir("lib") {
        zip("main.jar") {
          dir("META-INF") {
            file(
              "plugin.xml",
              pluginXml(
                """
                <xi:include xmlns:xi="http://www.w3.org/2001/XInclude" href="absent.xml">
                  <xi:fallback/>
                </xi:include>
                """.trimIndent()
              )
            )
          }
        }
      }
    }
    assertEquals(emptySet(), plugin.pluginAliases)
  }

  @Test
  fun `the inert default xpointer is accepted`() {
    // Selects all children of the root element, which is what an include does anyway, so both
    // parsers treat it as a no-op.
    val plugin = buildPluginSuccessfully("inert-xpointer") {
      dir("lib") {
        zip("main.jar") {
          dir("META-INF") {
            file(
              "plugin.xml",
              pluginXml(
                """<xi:include xmlns:xi="http://www.w3.org/2001/XInclude" href="extensions.xml" """ +
                  """xpointer="xpointer(/idea-plugin/*)"/>"""
              )
            )
            file("extensions.xml", aliasXml("alias.with.inert.xpointer"))
          }
        }
      }
    }
    assertEquals(setOf("alias.with.inert.xpointer"), plugin.pluginAliases)
  }

  @Test
  fun `any other xpointer value fails the plugin`() {
    val result = buildPlugin("other-xpointer") {
      dir("lib") {
        zip("main.jar") {
          dir("META-INF") {
            file(
              "plugin.xml",
              pluginXml(
                """<xi:include xmlns:xi="http://www.w3.org/2001/XInclude" href="extensions.xml" """ +
                  """xpointer="xpointer(/idea-plugin/extensions/*)"/>"""
              )
            )
            file("extensions.xml", aliasXml("never.read"))
          }
        }
      }
    }
    val fail = assertFailed(result)
    assertTrue(
      "expected the xpointer to be reported, got ${fail.errorsAndWarnings}",
      fail.errorsAndWarnings.any { it.message.contains("xpointer") }
    )
  }

  @Test
  fun `a conditional include on a non-allowlisted plugin is a named error`() {
    val result = buildPlugin("conditional-include") {
      dir("lib") {
        zip("main.jar") {
          dir("META-INF") {
            file(
              "plugin.xml",
              pluginXml(
                """<xi:include xmlns:xi="http://www.w3.org/2001/XInclude" href="extensions.xml" """ +
                  """includeIf="com.example.someProperty"/>"""
              )
            )
            file("extensions.xml", aliasXml("never.read"))
          }
        }
      }
    }
    val fail = assertFailed(result)
    val problem = fail.errorsAndWarnings.singleOrNull { it is ConditionalIncludeNotSupported }
    assertTrue("expected a ConditionalIncludeNotSupported, got ${fail.errorsAndWarnings}", problem != null)
    val message = problem!!.message
    // The message must name the attribute, the removal, and the compatibility that justifies failing.
    assertTrue(message, message.contains("includeIf"))
    assertTrue(message, message.contains("IJPL-215563"))
    assertTrue(message, message.contains("263.*"))
  }

  @Test
  fun `includeUnless is reported the same way as includeIf`() {
    val result = buildPlugin("conditional-include-unless") {
      dir("lib") {
        zip("main.jar") {
          dir("META-INF") {
            file(
              "plugin.xml",
              pluginXml(
                """<xi:include xmlns:xi="http://www.w3.org/2001/XInclude" href="extensions.xml" """ +
                  """includeUnless="com.example.someProperty"/>"""
              )
            )
            file("extensions.xml", aliasXml("never.read"))
          }
        }
      }
    }
    val fail = assertFailed(result)
    val problem = fail.errorsAndWarnings.singleOrNull { it is ConditionalIncludeNotSupported }
    assertTrue("expected a ConditionalIncludeNotSupported, got ${fail.errorsAndWarnings}", problem != null)
    assertTrue(problem!!.message, problem.message.contains("includeUnless"))
  }

  // --- fixtures ---------------------------------------------------------------------------------

  private fun include(href: String) =
    """<xi:include xmlns:xi="http://www.w3.org/2001/XInclude" href="$href"/>"""

  private fun aliasXml(alias: String) = ideaPlugin("""<module value="$alias"/>""")

  private fun ideaPlugin(body: String) = "<idea-plugin>\n  $body\n</idea-plugin>"

  /** `until-build="263.*"` is what puts this descriptor on the platform parser, see the class doc. */
  private fun pluginXml(additionalContent: String) = """
    <idea-plugin>
      <id>com.example.xinclude</id>
      <name>XInclude Example</name>
      <version>1.0</version>
      <vendor>Vendor Inc.</vendor>
      <description><![CDATA[A plugin exercising xi:include resolution on the platform parser path.]]></description>
      <idea-version since-build="241.0" until-build="263.*"/>
      <depends>com.intellij.modules.platform</depends>
      $additionalContent
    </idea-plugin>
  """.trimIndent()

  private fun buildPlugin(name: String, content: ContentBuilder.() -> Unit): PluginCreationResult<IdePlugin> {
    val pluginPath = buildDirectory(temporaryFolder.newFolder(name).toPath(), content)
    val manager = IdePluginManager.createManager(
      DefaultResourceResolver, temporaryFolder.newFolder().toPath(), SingletonCachingJarFileSystemProvider
    )
    return manager.createPlugin(pluginPath, false)
  }

  private fun buildPluginSuccessfully(name: String, content: ContentBuilder.() -> Unit): IdePlugin {
    val result = buildPlugin(name, content)
    assertTrue("expected a successfully created plugin but got $result", result is PluginCreationSuccess)
    return (result as PluginCreationSuccess).plugin
  }

  private fun assertFailed(result: PluginCreationResult<IdePlugin>): PluginCreationFail<IdePlugin> {
    assertTrue("expected a failed plugin creation but got $result", result is PluginCreationFail)
    return result as PluginCreationFail
  }

  private fun assertEquals(expected: Set<String>, actual: Set<String>) =
    assertEquals(expected as Any, actual as Any)
}
