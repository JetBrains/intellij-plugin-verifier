package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.ReclassifiedPluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.descriptors.DescriptorResource
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
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
    val uri = URI("file:///plugins/somePlugin/META-INF/plugin.xml")

    val descriptorResource = DescriptorResource(PLUGIN_XML.byteInputStream(), uri)
    descriptorResource.inputStream.use { stream ->
      val descriptorXml = JDOMUtil.loadDocument(stream)
      PluginCreator.createPlugin(
        descriptorResource,
        parentPlugin = null,
        descriptorXml,
        resourceResolver,
        problemResolver
      ).run {
        assertTrue(isSuccess)
        assertEquals(5, resolvedProblems.size)
      }
    }
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

private const val PLUGIN_XML = """
  <idea-plugin allow-bundled-update="true">
    <id>com.intellij.ml.llm</id>
    <version>251.SNAPSHOT</version>
    <idea-version since-build="251.SNAPSHOT" until-build="251.SNAPSHOT" />
    <name>JetBrains AI Assistant</name>
    <vendor>JetBrains</vendor>
    <description>JetBrains AI Assistant provides AI-powered features for software development based on the JetBrains AI Service.</description>
  </idea-plugin>
"""