package com.jetbrains.pluginverifier.output.marketplace

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dymamic.DynamicPluginStatus
import com.jetbrains.pluginverifier.jdk.JdkVersion
import com.jetbrains.pluginverifier.output.BaseOutputTest
import com.jetbrains.pluginverifier.output.PLUGIN_ID
import com.jetbrains.pluginverifier.output.PLUGIN_VERSION
import com.jetbrains.pluginverifier.response.VerificationResultTypeDto
import com.jetbrains.pluginverifier.response.prepareResponse
import org.junit.Test
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class FullResultResponseTest: BaseOutputTest() {

    private val ideVersion = IdeVersion.createIdeVersion("232")
    private val pluginInfo = mockPluginInfo()
    private val verificationTarget = PluginVerificationTarget.IDE(ideVersion, JdkVersion("11", null))
    private val dependenciesGraph: DependenciesGraph = DependenciesGraph(
        verifiedPlugin = DependencyNode(PLUGIN_ID, PLUGIN_VERSION),
        vertices = emptyList(),
        edges = emptyList(),
        missingDependencies = emptyMap())

    @Test
    fun `prepare response for critical problem result`() {
        val result = PluginVerificationResult.Verified(
            plugin = pluginInfo,
            verificationTarget = verificationTarget,
            dependenciesGraph = dependenciesGraph,
            compatibilityProblems = setOf(methodNotFoundProblem(), superInterfaceBecameClassProblem()),
            dynamicPluginStatus = DynamicPluginStatus.MaybeDynamic,
        )
        val response = result.prepareResponse(Random.nextInt(1, 10000), ideVersion.toString())
        assertEquals(VerificationResultTypeDto.CRITICAL, response.resultType)
        assertEquals(1, response.criticalCompatibilityProblems.size)
        assertEquals(1, response.compatibilityProblems.size)
    }

    @Test
    fun `prepare response for problem result`() {
        val result = PluginVerificationResult.Verified(
            plugin = pluginInfo,
            verificationTarget = verificationTarget,
            dependenciesGraph = dependenciesGraph,
            compatibilityProblems = setOf(superInterfaceBecameClassProblem()),
            dynamicPluginStatus = DynamicPluginStatus.MaybeDynamic,
        )
        val response = result.prepareResponse(Random.nextInt(1, 10000), ideVersion.toString())
        assertEquals(VerificationResultTypeDto.PROBLEMS, response.resultType)
        assertEquals(1, response.compatibilityProblems.size)
    }

    @Test
    fun `prepare response for problem result with internal api usages`() {
        val result = PluginVerificationResult.Verified(
            plugin = pluginInfo,
            verificationTarget = verificationTarget,
            dependenciesGraph = dependenciesGraph,
            internalApiUsages = internalApiUsages(),
            dynamicPluginStatus = DynamicPluginStatus.MaybeDynamic,
        )
        val response = result.prepareResponse(Random.nextInt(1, 10000), ideVersion.toString())
        assertEquals(VerificationResultTypeDto.PROBLEMS, response.resultType)
        assertTrue(response.compatibilityProblems.isEmpty())
        assertEquals(1, response.internalApiUsages.size)
    }

    @Test
    fun `prepare response for warning result`() {
        val result = PluginVerificationResult.Verified(
            plugin = pluginInfo,
            verificationTarget = verificationTarget,
            dependenciesGraph = dependenciesGraph,
            pluginStructureWarnings = mockStructureWarnings(),
            dynamicPluginStatus = DynamicPluginStatus.MaybeDynamic,
        )
        val response = result.prepareResponse(Random.nextInt(1, 10000), ideVersion.toString())
        assertEquals(VerificationResultTypeDto.WARNINGS, response.resultType)
        assertEquals(1, response.pluginStructureWarnings.size)
    }

    @Test
    fun `prepare response for ok result`() {
        val result = PluginVerificationResult.Verified(
            plugin = pluginInfo,
            verificationTarget = verificationTarget,
            dependenciesGraph = dependenciesGraph,
            dynamicPluginStatus = DynamicPluginStatus.MaybeDynamic,
        )
        val response = result.prepareResponse(Random.nextInt(1, 10000), ideVersion.toString())
        assertEquals(VerificationResultTypeDto.OK, response.resultType)
    }
}