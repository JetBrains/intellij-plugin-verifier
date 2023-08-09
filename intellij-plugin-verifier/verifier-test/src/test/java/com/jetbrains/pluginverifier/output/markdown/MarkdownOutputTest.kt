package com.jetbrains.pluginverifier.output.markdown

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dymamic.DynamicPluginStatus
import com.jetbrains.pluginverifier.dymamic.DynamicPluginStatus.MaybeDynamic
import com.jetbrains.pluginverifier.jdk.JdkVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.hierarchy.ClassHierarchy
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.modifiers.Modifiers.Modifier.PUBLIC
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.problems.MethodNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.SuperInterfaceBecameClassProblem
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.internal.InternalClassUsage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.PrintWriter
import java.io.StringWriter

private const val PLUGIN_ID = "pluginId"
private const val PLUGIN_VERSION = "1.0"

class MarkdownOutputTest {
  private val pluginInfo = mockPluginInfo(PLUGIN_ID, PLUGIN_VERSION)
  private val verificationTarget = PluginVerificationTarget.IDE(IdeVersion.createIdeVersion("232"), JdkVersion("11", null))

  @Test
  fun `plugin is compatible`() {
    val dependenciesGraph = DependenciesGraph(
      verifiedPlugin = DependencyNode(PLUGIN_ID, PLUGIN_VERSION),
      vertices = emptyList(),
      edges = emptyList(),
      missingDependencies = emptyMap()
    )
    val verificationResult = PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph)

    val out = StringWriter()
    val resultPrinter = MarkdownResultPrinter(PrintWriter(out))
    resultPrinter.printResults(listOf(verificationResult))

    val expected = """
      # Plugin pluginId $PLUGIN_VERSION against 232.0
      
      Compatible
      
      
      """.trimIndent()
    assertEquals(expected, out.buffer.toString())
  }

  @Test
  fun `plugin has compatibility warnings`() {
    val dependenciesGraph = DependenciesGraph(
      verifiedPlugin = DependencyNode(PLUGIN_ID, PLUGIN_VERSION),
      vertices = emptyList(),
      edges = emptyList(),
      missingDependencies = emptyMap()
    )
    val verificationResult = PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph, mockCompatibilityProblems())

    val out = StringWriter()
    val resultPrinter = MarkdownResultPrinter(PrintWriter(out))
    resultPrinter.printResults(listOf(verificationResult))

    val expected = """
      # Plugin pluginId 1.0 against 232.0
      
      4 compatibility problems
      
      ## Compatibility problems (4): 
      
      ### Incompatible change of super interface com.jetbrains.plugin.Parent to class
      
      * Class com.jetbrains.plugin.Child has a *super interface* com.jetbrains.plugin.Parent which is actually a *class*. This can lead to **IncompatibleClassChangeError** exception at runtime.
      
      ### Incompatible change of super interface com.jetbrains.plugin.pkg.Parent to class
      
      * Class com.jetbrains.plugin.pkg.Child has a *super interface* com.jetbrains.plugin.pkg.Parent which is actually a *class*. This can lead to **IncompatibleClassChangeError** exception at runtime.
      
      ### Invocation of unresolved method org.some.deleted.Class.foo() : void
      
      * Method SomeClass.someMethod() : void contains an *invokevirtual* instruction referencing an unresolved method org.some.deleted.Class.foo() : void. This can lead to **NoSuchMethodError** exception at runtime.
      * Method VioletClass.produceViolet() : void contains an *invokevirtual* instruction referencing an unresolved method org.some.deleted.Class.foo() : void. This can lead to **NoSuchMethodError** exception at runtime.


      """.trimIndent()
    assertEquals(expected, out.buffer.toString())
  }

  @Test
  fun `plugin has internal API usages problems`() {
    val dependenciesGraph = DependenciesGraph(
      verifiedPlugin = DependencyNode(PLUGIN_ID, PLUGIN_VERSION),
      vertices = emptyList(),
      edges = emptyList(),
      missingDependencies = emptyMap()
    )

    val internalApiUsages = setOf(
      InternalClassUsage(ClassReference("com.jetbrains.InternalClass"), internalApiClassLocation, mockMethodLocationInVioletClass)
    )

    val verificationResult = PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph, internalApiUsages = internalApiUsages)

    val out = StringWriter()
    val resultPrinter = MarkdownResultPrinter(PrintWriter(out))
    resultPrinter.printResults(listOf(verificationResult))

    val expected = """
        # Plugin pluginId 1.0 against 232.0
        
        Compatible. 1 usage of internal API
        
        ## Internal API usages (1): 
        
        ### Internal class InternalApiRegistrar reference
        
        * Internal class InternalApiRegistrar is referenced in VioletClass.produceViolet() : void. This class is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the class is not supposed to be used in client code.


      """.trimIndent()
    assertEquals(expected, out.buffer.toString())
  }

  @Test
  fun `plugin is dynamic`() {
    val dependenciesGraph = DependenciesGraph(
      verifiedPlugin = DependencyNode(PLUGIN_ID, PLUGIN_VERSION),
      vertices = emptyList(),
      edges = emptyList(),
      missingDependencies = emptyMap()
    )

    val verificationResult = PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph, dynamicPluginStatus = MaybeDynamic)

    val out = StringWriter()
    val resultPrinter = MarkdownResultPrinter(PrintWriter(out))
    resultPrinter.printResults(listOf(verificationResult))

    val expected = """
# Plugin pluginId 1.0 against 232.0

Compatible

## Dynamic Plugin Status

Plugin can probably be enabled or disabled without IDE restart


      """.trimIndent()
    assertEquals(expected, out.buffer.toString())
  }
}

fun mockPluginInfo(pluginId: String, version: String): PluginInfo =
  object : PluginInfo(pluginId, pluginId, version, null, null, null) {}

fun mockCompatibilityProblems(): Set<CompatibilityProblem> =
  setOf(superInterfaceBecameClassProblem(), superInterfaceBecameClassProblemInOtherLocation(), methodNotFoundProblem(), methodNotFoundProblemInVioletClass())

fun superInterfaceBecameClassProblem(): SuperInterfaceBecameClassProblem {
  val child = ClassLocation("com.jetbrains.plugin.Child", null, Modifiers.of(PUBLIC), SomeFileOrigin)
  val clazz = ClassLocation("com.jetbrains.plugin.Parent", null, Modifiers.of(PUBLIC), SomeFileOrigin)
  return SuperInterfaceBecameClassProblem(child, clazz)
}

fun superInterfaceBecameClassProblemInOtherLocation(): SuperInterfaceBecameClassProblem {
  val child = ClassLocation("com.jetbrains.plugin.pkg.Child", null, Modifiers.of(PUBLIC), SomeFileOrigin)
  val clazz = ClassLocation("com.jetbrains.plugin.pkg.Parent", null, Modifiers.of(PUBLIC), SomeFileOrigin)
  return SuperInterfaceBecameClassProblem(child, clazz)
}


//FIXME consolidate with DocumentedProblemsReportingTest
val mockMethodLocation = MethodLocation(
  ClassLocation("SomeClass", null, Modifiers.of(PUBLIC), SomeFileOrigin),
  "someMethod",
  "()V",
  emptyList(),
  null,
  Modifiers.of(PUBLIC)
)

private val violetClassLocation = ClassLocation("VioletClass", null, Modifiers.of(PUBLIC), SomeFileOrigin)
private val internalApiClassLocation = ClassLocation("InternalApiRegistrar", null, Modifiers.of(PUBLIC), SomeFileOrigin)

val mockMethodLocationInVioletClass = MethodLocation(
  violetClassLocation,
  "produceViolet",
  "()V",
  emptyList(),
  null,
  Modifiers.of(PUBLIC)
)

fun methodNotFoundProblem(): MethodNotFoundProblem {
  //FIXME consolidate with DocumentedProblemsReportingTest
  val JAVA_LANG_OBJECT_HIERARCHY = ClassHierarchy(
    "java/lang/Object",
    false,
    null,
    emptyList()
  )

  val deletedClassRef = ClassReference("org/some/deleted/Class")
  return MethodNotFoundProblem(
    MethodReference(deletedClassRef, "foo", "()V"),
    mockMethodLocation,
    Instruction.INVOKE_VIRTUAL,
    JAVA_LANG_OBJECT_HIERARCHY
  )
}

fun methodNotFoundProblemInVioletClass(): MethodNotFoundProblem {
  //FIXME consolidate with DocumentedProblemsReportingTest
  val JAVA_LANG_OBJECT_HIERARCHY = ClassHierarchy(
    "java/lang/Object",
    false,
    null,
    emptyList()
  )

  val deletedClassRef = ClassReference("org/some/deleted/Class")
  return MethodNotFoundProblem(
    MethodReference(deletedClassRef, "foo", "()V"),
    mockMethodLocationInVioletClass,
    Instruction.INVOKE_VIRTUAL,
    JAVA_LANG_OBJECT_HIERARCHY
  )
}


private object SomeFileOrigin : FileOrigin {
  override val parent: FileOrigin? = null
}