package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.ReclassifiedPluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.descriptors.DescriptorResource
import com.jetbrains.plugin.structure.intellij.problems.ForbiddenPluginIdPrefix
import com.jetbrains.plugin.structure.intellij.problems.NoDependencies
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.TemplateWordInPluginId
import com.jetbrains.plugin.structure.intellij.problems.TemplateWordInPluginName
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.nio.file.Path

class PluginCreatorTest {
  @Test
  fun `plugin is created from the descriptor resource`() {
    val expectedProblems = listOf(
      ForbiddenPluginIdPrefix::class,
      TemplateWordInPluginId::class,
      TemplateWordInPluginName::class,
      NoDependencies::class)

    val plugin = createPlugin(DUMMY_PLUGIN_XML)
    plugin.run {
      assertTrue(isSuccess)
      assertEquals(4, resolvedProblems.size)
      expectedProblems.forEach { problemClass ->
        assertTrue("$problemClass must be detected",
                   resolvedProblems.any { it::class == problemClass })
      }
    }
  }

  @Test
  fun `content modules`() {
    val pluginCreator = createPlugin(PLUGIN_XML_WITH_CONTENT_MODULES)
    with(pluginCreator.plugin.contentModules) {
      assertEquals(6,  size)
      with(this[0]) {
        assertEquals("my.optional.module", name, )
        assertEquals(ModuleLoadingRule.OPTIONAL, loadingRule)
      }
      with(this[1]) {
        assertEquals("my.required.module", name)
        assertEquals(ModuleLoadingRule.REQUIRED, loadingRule)
      }
      with(this[2]) {
        assertEquals("my.embedded.module", name)
        assertEquals(ModuleLoadingRule.EMBEDDED, loadingRule)
      }
      with(this[3]) {
        assertEquals("my.explicit.optional.module", name)
        assertEquals(ModuleLoadingRule.OPTIONAL, loadingRule)
      }
      with(this[4]) {
        assertEquals("my.on.demand.module", name)
        assertEquals(ModuleLoadingRule.ON_DEMAND, loadingRule)
      }
      with(this[5]) {
        assertEquals("my.unknown.module", name)
        assertEquals("unknown", loadingRule.id)
      }
    }
  }

  private fun createPlugin(pluginXml: String): PluginCreator {
    val uri = URI("file:///plugins/somePlugin/META-INF/plugin.xml")

    val descriptorResource = DescriptorResource(pluginXml.byteInputStream(), uri)
    val plugin = descriptorResource.inputStream.use { stream ->
      val descriptorXml = JDOMUtil.loadDocument(stream)
      PluginCreator.createPlugin(
        descriptorResource,
        parentPlugin = null,
        descriptorXml,
        resourceResolver,
        problemResolver
      )
    }
    return plugin
  }

  private val resourceResolver = object : ResourceResolver {
    override fun resolveResource(
      relativePath: String,
      basePath: Path
    ) = ResourceResolver.Result.NotFound
  }

  private val problemResolver = object : PluginCreationResultResolver {
    override fun resolve(
      plugin: IdePlugin,
      problems: List<PluginProblem>
    ): PluginCreationResult<IdePlugin> {
      val problems = problems.map { problem ->
        if (problem.level == PluginProblem.Level.ERROR) {
          ReclassifiedPluginProblem(PluginProblem.Level.WARNING, problem)
        } else {
          problem
        }
      }
      return PluginCreationSuccess(plugin, problems)
    }
  }
}

private const val DUMMY_PLUGIN_XML = """
  <idea-plugin allow-bundled-update="true">
    <id>com.intellij.ml.llm</id>
    <version>251.SNAPSHOT</version>
    <idea-version since-build="251.SNAPSHOT" until-build="251.SNAPSHOT" />
    <name>JetBrains AI Assistant</name>
    <vendor>JetBrains</vendor>
    <description>JetBrains AI Assistant provides AI-powered features for software development based on the JetBrains AI Service.</description>
  </idea-plugin>
"""

private const val PLUGIN_XML_WITH_CONTENT_MODULES = """
  <idea-plugin>
     <id>my.plugin</id>
     <name>My Plugin</name>
     <version>1.0</version>
     <vendor>Me</vendor>
     <description>My plugin</description>
     <idea-version since-build="251.0"/>
     <content>
       <module name="my.optional.module"/>
       <module name="my.required.module" loading="required"/>
       <module name="my.embedded.module" loading="embedded"/>
       <module name="my.explicit.optional.module" loading="optional"/>
       <module name="my.on.demand.module" loading="on-demand"/>
       <module name="my.unknown.module" loading="unknown"/>
     </content>
  </idea-plugin> 
"""