package com.jetbrains.pluginverifier.output

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.problems.ForbiddenPluginIdPrefix
import com.jetbrains.plugin.structure.intellij.problems.NoModuleDependencies
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.dymamic.DynamicPluginStatus
import com.jetbrains.pluginverifier.jdk.JdkVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.hierarchy.ClassHierarchy
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.location.toReference
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.problems.MethodNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.SuperInterfaceBecameClassProblem
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.tasks.InvalidPluginFile
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalClassUsage
import com.jetbrains.pluginverifier.usages.internal.InternalClassUsage
import com.jetbrains.pluginverifier.usages.nonExtendable.NonExtendableTypeInherited
import com.jetbrains.pluginverifier.warnings.PluginStructureWarning
import org.junit.Assert.assertEquals
import java.io.StringWriter
import kotlin.io.path.Path

const val PLUGIN_ID = "pluginId"
const val PLUGIN_VERSION = "1.0"

typealias VerifiedPluginHandler = (PluginVerificationResult.Verified) -> Unit

open class BaseOutputTest<T : ResultPrinter> {
  private val pluginInfo = mockPluginInfo()
  private val verificationTarget = PluginVerificationTarget.IDE(IdeVersion.createIdeVersion("232"), JdkVersion("11", null))

  protected lateinit var out: StringWriter
  protected lateinit var resultPrinter: T

  fun interface VerifiedPluginWithPrinterRunner<T, R> {
    fun run(resultPrinter: T, result: R)
  }

  open fun setUp() {
    out = StringWriter()
  }

  private val dependenciesGraph: DependenciesGraph = DependenciesGraph(
        verifiedPlugin = DependencyNode(PLUGIN_ID, PLUGIN_VERSION),
        vertices = emptyList(),
        edges = emptyList(),
        missingDependencies = emptyMap())

  open fun `when plugin is compatible`(testRunner: VerifiedPluginHandler) {
    testRunner.runTest(PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph))
  }

  open fun `when plugin has compatibility warnings`(testRunner: VerifiedPluginHandler) {
    testRunner.runTest(PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph, mockCompatibilityProblems()))
  }

  open fun `when plugin has structural problems`(testRunner: VerifiedPluginHandler) {
    val structureWarnings = setOf(
      PluginStructureWarning(NoModuleDependencies(IdePluginManager.PLUGIN_XML))
    )
    testRunner.runTest(PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph, pluginStructureWarnings = structureWarnings))
  }

  open fun `when plugin has internal API usage problems`(testRunner: VerifiedPluginHandler) {
    val internalApiUsages = setOf(
      InternalClassUsage(ClassReference("com.jetbrains.InternalClass"), internalApiClassLocation, mockMethodLocationInSampleStuffFactory)
    )

    testRunner.runTest(PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph, internalApiUsages = internalApiUsages))
  }

  open fun `when plugin has non-extendable API usages problems`(testRunner: VerifiedPluginHandler) {
    val nonExtendableClass = ClassLocation("NonExtendableClass", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)
    val extendingClass = ClassLocation("ExtendingClass", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)

    val nonExtendableApiUsages = setOf(
      NonExtendableTypeInherited(nonExtendableClass, extendingClass)
    )

    testRunner.runTest(PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph, nonExtendableApiUsages = nonExtendableApiUsages))
  }

  open fun `when plugin has experimental API usage problems`(testRunner: VerifiedPluginHandler) {
    val experimentalClass = ClassLocation("ExperimentalClass", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)
    val extendingClass = ClassLocation("ExtendingClass", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)
    val usageLocation = MethodLocation(
      extendingClass,
      "someMethod",
      "()V",
      emptyList(),
      null,
      Modifiers.of(Modifiers.Modifier.PUBLIC)
    )

    val experimentalApiUsages = setOf(
      ExperimentalClassUsage(experimentalClass.toReference(), experimentalClass, usageLocation)
    )

    testRunner.runTest(PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph, experimentalApiUsages = experimentalApiUsages))
  }

  open fun `when plugin has missing dependencies`(testRunner: VerifiedPluginHandler) {
    val pluginDependency = DependencyNode(PLUGIN_ID, PLUGIN_VERSION)
    val expectedDependency = MissingDependency(PluginDependencyImpl("MissingPlugin", true, false), "Dependency MissingPlugin is not found among the bundled plugins of IU-211.500")

    val dependenciesGraph = DependenciesGraph(
      verifiedPlugin = pluginDependency,
      vertices = emptyList(),
      edges = emptyList(),
      missingDependencies = mapOf(pluginDependency to setOf(expectedDependency))
    )

    testRunner.runTest(PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph))
  }

  open fun `when plugin is dynamic`(testRunner: VerifiedPluginHandler) {
    testRunner.runTest(PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph, dynamicPluginStatus = DynamicPluginStatus.MaybeDynamic))
  }

  open fun `when plugin is dynamic and has structural warnings`(testRunner: VerifiedPluginHandler) {
    val structureWarnings = setOf(
      PluginStructureWarning(NoModuleDependencies(IdePluginManager.PLUGIN_XML))
    )
    testRunner.runTest(PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph,
      dynamicPluginStatus = DynamicPluginStatus.MaybeDynamic,
      pluginStructureWarnings = structureWarnings
    ))
  }

  open fun `when plugin has structural problems with invalid plugin ID`(testRunner: VerifiedPluginWithPrinterRunner<T, List<InvalidPluginFile>>) {
    val pluginId = "com.example.intellij"
    val prefix = "com.example"
    val invalidPluginFiles = listOf(
      InvalidPluginFile(Path("plugin.zip"), listOf(ForbiddenPluginIdPrefix(pluginId, prefix)))
    )

    testRunner.run(resultPrinter, invalidPluginFiles)
  }

  fun output() = out.buffer.toString()

  private fun printResults(verificationResult: PluginVerificationResult) {
    resultPrinter.printResults(listOf(verificationResult))
  }

  fun assertOutput(expected: String) {
    assertEquals(expected, output())
  }

  private fun VerifiedPluginHandler.runTest(verificationResult: PluginVerificationResult.Verified) {
    printResults(verificationResult)
    this(verificationResult)
  }
}

private fun mockPluginInfo(): PluginInfo =
  object : PluginInfo(PLUGIN_ID, PLUGIN_ID, PLUGIN_VERSION, null, null, null) {}

private fun mockCompatibilityProblems(): Set<CompatibilityProblem> =
  setOf(superInterfaceBecameClassProblem(), superInterfaceBecameClassProblemInOtherLocation(), methodNotFoundProblem(), methodNotFoundProblemInSampleStuffFactoryClass())

private fun superInterfaceBecameClassProblem(): SuperInterfaceBecameClassProblem {
  val child = ClassLocation("com.jetbrains.plugin.Child", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)
  val clazz = ClassLocation("com.jetbrains.plugin.Parent", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)
  return SuperInterfaceBecameClassProblem(child, clazz)
}

private fun superInterfaceBecameClassProblemInOtherLocation(): SuperInterfaceBecameClassProblem {
  val child = ClassLocation("com.jetbrains.plugin.pkg.Child", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)
  val clazz = ClassLocation("com.jetbrains.plugin.pkg.Parent", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)
  return SuperInterfaceBecameClassProblem(child, clazz)
}

private val javaLangObjectClassHierarchy = ClassHierarchy(
  "java/lang/Object",
  false,
  null,
  emptyList()
)

private val sampleStuffFactoryLocation = ClassLocation("SampleStuffFactory", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)
private val internalApiClassLocation = ClassLocation("InternalApiRegistrar", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)

private val mockMethodLocationInSampleStuffFactory = MethodLocation(
  sampleStuffFactoryLocation,
  "produceStuff",
  "()V",
  emptyList(),
  null,
  Modifiers.of(Modifiers.Modifier.PUBLIC)
)

private fun methodNotFoundProblem(): MethodNotFoundProblem {
  val deletedClassRef = ClassReference("org/some/deleted/Class")
  val referencingMethodLocation = MethodLocation(
    ClassLocation("SomeClassUsingDeletedClass", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin),
    "someMethodReferencingDeletedClass",
    "()V",
    emptyList(),
    null,
    Modifiers.of(Modifiers.Modifier.PUBLIC)
  )
  return MethodNotFoundProblem(
    MethodReference(deletedClassRef, "foo", "()V"),
    referencingMethodLocation,
    Instruction.INVOKE_VIRTUAL,
    javaLangObjectClassHierarchy
  )
}

private fun methodNotFoundProblemInSampleStuffFactoryClass(): MethodNotFoundProblem {
  val deletedClassRef = ClassReference("org/some/deleted/Class")
  return MethodNotFoundProblem(
    MethodReference(deletedClassRef, "foo", "()V"),
    mockMethodLocationInSampleStuffFactory,
    Instruction.INVOKE_VIRTUAL,
    javaLangObjectClassHierarchy
  )
}

private object SomeFileOrigin : FileOrigin {
  override val parent: FileOrigin? = null
}