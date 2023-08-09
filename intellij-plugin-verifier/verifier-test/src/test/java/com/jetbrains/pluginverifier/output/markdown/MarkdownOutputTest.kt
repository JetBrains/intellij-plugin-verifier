package com.jetbrains.pluginverifier.output.markdown

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.problems.NoModuleDependencies
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.dymamic.DynamicPluginStatus.MaybeDynamic
import com.jetbrains.pluginverifier.jdk.JdkVersion
import com.jetbrains.pluginverifier.output.ResultPrinter
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.hierarchy.ClassHierarchy
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.location.toReference
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.modifiers.Modifiers.Modifier.PUBLIC
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.problems.MethodNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.SuperInterfaceBecameClassProblem
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalClassUsage
import com.jetbrains.pluginverifier.usages.internal.InternalClassUsage
import com.jetbrains.pluginverifier.usages.nonExtendable.NonExtendableTypeInherited
import com.jetbrains.pluginverifier.warnings.PluginStructureWarning
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.PrintWriter
import java.io.StringWriter
import java.io.Writer

private const val PLUGIN_ID = "pluginId"
private const val PLUGIN_VERSION = "1.0"

class MarkdownOutputTest {
  private val pluginInfo = mockPluginInfo(PLUGIN_ID, PLUGIN_VERSION)
  private val verificationTarget = PluginVerificationTarget.IDE(IdeVersion.createIdeVersion("232"), JdkVersion("11", null))

  private lateinit var out: StringWriter
  private lateinit var resultPrinter: ResultPrinter

  @Before
  fun setUp() {
    out = StringWriter()
    resultPrinter = MarkdownResultPrinter(PrintWriter(out))
  }

  @Test
  fun `plugin is compatible`() {
    val dependenciesGraph = DependenciesGraph(
      verifiedPlugin = DependencyNode(PLUGIN_ID, PLUGIN_VERSION),
      vertices = emptyList(),
      edges = emptyList(),
      missingDependencies = emptyMap()
    )
    val verificationResult = PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph)
    resultPrinter.printResults(listOf(verificationResult))

    val expected = """
      # Plugin pluginId $PLUGIN_VERSION against 232.0
      
      Compatible
      
      
      """.trimIndent()
    assertEquals(expected, output())
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
    assertEquals(expected, output())
  }

  @Test
  fun `plugin has structural problems`() {
    val dependenciesGraph = DependenciesGraph(
      verifiedPlugin = DependencyNode(PLUGIN_ID, PLUGIN_VERSION),
      vertices = emptyList(),
      edges = emptyList(),
      missingDependencies = emptyMap()
    )


    val structureWarnings = setOf(
      PluginStructureWarning(NoModuleDependencies(IdePluginManager.PLUGIN_XML))
    )
    val verificationResult = PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph, pluginStructureWarnings = structureWarnings)

    resultPrinter.printResults(listOf(verificationResult))

    val expected = """
          # Plugin pluginId 1.0 against 232.0
          
          Compatible. 1 plugin configuration defect
          
          ## Plugin structure warnings (1): 
          
          * Plugin descriptor plugin.xml does not include any module dependency tags. The plugin is assumed to be a legacy plugin and is loaded only in IntelliJ IDEA. See https://plugins.jetbrains.com/docs/intellij/plugin-compatibility.html


      """.trimIndent()
    assertEquals(expected, output())
  }

  @Test
  fun `plugin has internal API usage problems`() {
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

    resultPrinter.printResults(listOf(verificationResult))

    val expected = """
        # Plugin pluginId 1.0 against 232.0
        
        Compatible. 1 usage of internal API
        
        ## Internal API usages (1): 
        
        ### Internal class InternalApiRegistrar reference
        
        * Internal class InternalApiRegistrar is referenced in VioletClass.produceViolet() : void. This class is marked with @org.jetbrains.annotations.ApiStatus.Internal annotation or @com.intellij.openapi.util.IntellijInternalApi annotation and indicates that the class is not supposed to be used in client code.


      """.trimIndent()
    assertEquals(expected, output())
  }

  @Test
  fun `plugin has non-extendable API usages problems`() {
    val dependenciesGraph = DependenciesGraph(
      verifiedPlugin = DependencyNode(PLUGIN_ID, PLUGIN_VERSION),
      vertices = emptyList(),
      edges = emptyList(),
      missingDependencies = emptyMap()
    )

    val nonExtendableClass = ClassLocation("NonExtendableClass", null, Modifiers.of(PUBLIC), SomeFileOrigin)
    val extendingClass = ClassLocation("ExtendingClass", null, Modifiers.of(PUBLIC), SomeFileOrigin)

    val nonExtendableApiUsages = setOf(
      NonExtendableTypeInherited(nonExtendableClass, extendingClass)
    )

    val verificationResult = PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph, nonExtendableApiUsages = nonExtendableApiUsages)

    resultPrinter.printResults(listOf(verificationResult))

    val expected = """
        # Plugin pluginId 1.0 against 232.0
        
        Compatible. 1 non-extendable API usage violation
        
        ## Non-extendable API usages (1): 
        
        ### Non-extendable class NonExtendableClass is extended
        
        * Non-extendable class NonExtendableClass is extended by ExtendingClass. This class is marked with @org.jetbrains.annotations.ApiStatus.NonExtendable, which indicates that the class is not supposed to be extended. See documentation of the @ApiStatus.NonExtendable for more info.


      """.trimIndent()
    assertEquals(expected, output())
  }

  @Test
  fun `plugin has experimental API usage problems`() {
    val dependenciesGraph = DependenciesGraph(
      verifiedPlugin = DependencyNode(PLUGIN_ID, PLUGIN_VERSION),
      vertices = emptyList(),
      edges = emptyList(),
      missingDependencies = emptyMap()
    )

    val experimentalClass = ClassLocation("ExperimentalClass", null, Modifiers.of(PUBLIC), SomeFileOrigin)
    val extendingClass = ClassLocation("ExtendingClass", null, Modifiers.of(PUBLIC), SomeFileOrigin)
    val usageLocation = MethodLocation(
      extendingClass,
      "someMethod",
      "()V",
      emptyList(),
      null,
      Modifiers.of(PUBLIC)
    )

    val experimentalApiUsages = setOf(
      ExperimentalClassUsage(experimentalClass.toReference(), experimentalClass, usageLocation)
    )

    val verificationResult = PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph, experimentalApiUsages = experimentalApiUsages)

    resultPrinter.printResults(listOf(verificationResult))

    val expected = """
          # Plugin pluginId 1.0 against 232.0
          
          Compatible. 1 usage of experimental API
          
          ## Experimental API usages (1): 
          
          ### Experimental API class ExperimentalClass reference
          
          * Experimental API class ExperimentalClass is referenced in ExtendingClass.someMethod() : void. This class can be changed in a future release leading to incompatibilities
          

      """.trimIndent()
    assertEquals(expected, output())
  }

  @Test
  fun `plugin has missing dependencies`() {
    val pluginDependency = DependencyNode(PLUGIN_ID, PLUGIN_VERSION)
    val expectedDependency = MissingDependency(PluginDependencyImpl("MissingPlugin", true, false), "Dependency MissingPlugin is not found among the bundled plugins of IU-211.500")

    val dependenciesGraph = DependenciesGraph(
      verifiedPlugin = pluginDependency,
      vertices = emptyList(),
      edges = emptyList(),
      missingDependencies = mapOf(pluginDependency to setOf(expectedDependency))
    )

    val verificationResult = PluginVerificationResult.Verified(pluginInfo, verificationTarget, dependenciesGraph)

    resultPrinter.printResults(listOf(verificationResult))

    val expected = """
        # Plugin pluginId 1.0 against 232.0
        
        Compatible
        
        ## Missing dependencies (1): 
        
        * MissingPlugin (optional): Dependency MissingPlugin is not found among the bundled plugins of IU-211.500
        
        
      """.trimIndent()
    assertEquals(expected, output())
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

    resultPrinter.printResults(listOf(verificationResult))

    val expected = """
        # Plugin pluginId 1.0 against 232.0
        
        Compatible
        
        ## Dynamic Plugin Status
        
        Plugin can probably be enabled or disabled without IDE restart


      """.trimIndent()
    assertEquals(expected, output())
  }

  private fun output() = out.buffer.toString()
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