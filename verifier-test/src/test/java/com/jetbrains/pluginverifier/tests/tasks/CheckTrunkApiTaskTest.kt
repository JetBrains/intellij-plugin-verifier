package com.jetbrains.pluginverifier.tests.tasks

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.version.IdeVersion.createIdeVersion
import com.jetbrains.pluginverifier.parameters.ide.IdeDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportageImpl
import com.jetbrains.pluginverifier.repository.local.LocalPluginInfo
import com.jetbrains.pluginverifier.repository.local.LocalPluginRepository
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.tasks.checkTrunkApi.CheckTrunkApiParams
import com.jetbrains.pluginverifier.tasks.checkTrunkApi.CheckTrunkApiTask
import com.jetbrains.pluginverifier.tests.mocks.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Created by Sergey.Patrikeev
 */
class CheckTrunkApiTaskTest {
  @Test
  fun `local plugins repository is used and the local plugins are resolved`() {
    val releaseVersion = createIdeVersion("IU-173.1")
    val trunkVersion = createIdeVersion("IU-181.1")

    val releaseIde = MockIde(releaseVersion)
    val trunkIde = MockIde(trunkVersion)

    val someJetbrainsPluginId = "org.jetbrains.plugin"

    val pluginToCheckFile = File("plugin to check")

    val someJetBrainsMockPlugin1 = MockIdePlugin(
        pluginId = someJetbrainsPluginId,
        pluginVersion = "1.0"
    )

    val someJetBrainsMockPlugin2 = MockIdePlugin(
        pluginId = someJetbrainsPluginId,
        pluginVersion = "2.0"
    )

    val pluginToCheck = MockIdePlugin(
        pluginId = "plugin.to.check",
        pluginVersion = "1.0",
        dependencies = listOf(PluginDependencyImpl(someJetbrainsPluginId, false, false))
    )

    val jetBrainsPluginFile1 = File("jetbrains.1.0")
    val jetBrainsPluginFile2 = File("jetbrains.2.0")

    val checkTrunkApiTask = CheckTrunkApiTask(
        CheckTrunkApiParams(
            IdeDescriptor(releaseIde, EmptyResolver),
            IdeDescriptor(trunkIde, EmptyResolver),
            emptyList(),
            emptyList(),
            TestJdkDescriptorProvider.getJdkDescriptorForTests(),
            listOf(someJetbrainsPluginId),
            false,
            File("release ide file"),
            LocalPluginRepository(releaseVersion, listOf(
                LocalPluginInfo(
                    someJetbrainsPluginId,
                    "1.0",
                    "plugin name",
                    releaseVersion,
                    releaseVersion,
                    "JetBrains",
                    jetBrainsPluginFile1
                )
            )),
            LocalPluginRepository(trunkVersion, listOf(
                LocalPluginInfo(
                    someJetbrainsPluginId,
                    "2.0",
                    "plugin name",
                    trunkVersion,
                    trunkVersion,
                    "JetBrains",
                    jetBrainsPluginFile2
                )
            )),
            listOf(PluginCoordinate.ByFile(pluginToCheckFile))
        ),
        EmptyPublicPluginRepository,
        MockPluginDetailsProvider(
            mapOf(
                PluginCoordinate.ByFile(jetBrainsPluginFile1) to PluginDetails.FoundOpenPluginWithoutClasses(
                    someJetBrainsMockPlugin1
                ),
                PluginCoordinate.ByFile(jetBrainsPluginFile2) to PluginDetails.FoundOpenPluginWithoutClasses(
                    someJetBrainsMockPlugin2
                ),
                PluginCoordinate.ByFile(pluginToCheckFile) to PluginDetails.FoundOpenPluginWithoutClasses(
                    pluginToCheck
                )
            )
        )
    )

    val verificationReportage = VerificationReportageImpl(EmptyReporterSetProvider)

    val checkTrunkApiResult = checkTrunkApiTask.execute(verificationReportage)
    val (trunkResult, releaseResult) = checkTrunkApiResult
    val trunkVerdict = trunkResult.results.single().verdict as Verdict.OK
    val releaseVerdict = releaseResult.results.single().verdict as Verdict.OK

    val trunkGraph = trunkVerdict.dependenciesGraph
    val releaseGraph = releaseVerdict.dependenciesGraph

    val trunkJBUsed = trunkGraph.vertices.find { it.id == someJetbrainsPluginId }!!
    val releaseJBUsed = releaseGraph.vertices.find { it.id == someJetbrainsPluginId }!!

    assertEquals("1.0", releaseJBUsed.version)
    assertEquals("2.0", trunkJBUsed.version)
  }


}