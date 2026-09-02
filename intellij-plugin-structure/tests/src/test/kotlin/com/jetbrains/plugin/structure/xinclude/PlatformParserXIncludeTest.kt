/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.xinclude

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.problems.ConditionalIncludeNotSupported
import com.jetbrains.plugin.structure.intellij.resources.DefaultResourceResolver
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * `<xi:include>` resolution on the platform-parser path, which resolves includes through the library's
 * own engine driven by
 * [ResourceResolverXIncludeLoader][com.jetbrains.plugin.structure.intellij.xinclude.ResourceResolverXIncludeLoader]
 * rather than through [XIncluder].
 *
 * These fixtures carry the weight of covering that codepath: the selection rule
 * ([com.jetbrains.plugin.structure.intellij.plugin.PluginCreator.shouldUsePlatformParser]) picks a small
 * minority of real plugins, and none of the ones it picks today uses `<xi:include>` at all. Every
 * descriptor below therefore declares `until-build="263.*"` so that it is selected on the rule's own
 * merits, not merely because the parser is currently forced on for everything.
 *
 * Each include contributes a plugin alias, which is simply the cheapest thing to observe: an alias
 * appears in [IdePlugin.pluginAliases] if and only if the included document was really read and merged.
 */
class PlatformParserXIncludeTest {
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
    The `plugins/Kotlin` shape, and the reason this codepath was rewritten to go through the plugin's
    existing composite ResourceResolver chain: the included descriptor lives in a DIFFERENT jar of the
    `lib` directory than the descriptor referencing it. A loader that only ever looked inside the
    artifact holding plugin.xml could not see it at all.
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
    The library treats `<xi:fallback>` purely as a marker meaning "this include is optional": on a
    missing target it consults the marker, and then `skipElement()`s the fallback WITHOUT merging its
    content (verified in XmlReader.readInclude's bytecode - the fallback branch is a skip, never a
    consume). So the plugin survives the missing include, but the fallback body contributes nothing.

    This is a real divergence from XIncluder, which does inline the fallback's children, and it matters
    for the plugins that put substantive content in a fallback rather than an empty <xi:fallback/>.

    It could not be observed on this branch before: with no XIncludeLoader installed at all, the library
    threw on the null loader before reading a single attribute, so an optional include failed just as
    hard as a required one.
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
    // The only xpointer value observed in the wild - it selects all children of the root element,
    // which is what an include does anyway, so both parsers treat it as a no-op.
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

  /**
   * `until-build` is what puts this descriptor on the platform parser under the real selection rule,
   * so these tests keep covering the include machinery even once the parser stops being forced on.
   */
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
