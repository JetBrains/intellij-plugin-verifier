import SerializationTest.Companion
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyEdge
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.persistence.VerificationResultPersistence
import com.jetbrains.pluginverifier.repository.repositories.custom.CustomPluginInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedMethodUsage
import com.jetbrains.pluginverifier.results.deprecated.DeprecationInfo
import com.jetbrains.pluginverifier.results.experimental.ExperimentalMethodUsage
import com.jetbrains.pluginverifier.results.hierarchy.ClassHierarchy
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.problems.MethodNotFoundProblem
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.results.structure.PluginStructureError
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileOrigin
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.URL

/**
 * Ensures that [VerificationResult] and relevant classes can be serialized and deserialized.
 *
 * It constructs a mock verification result using placeholders declared in [Companion],
 * serializes it using Java Serializable mechanism, deserialize's it and checks equality.
 */
class SerializationTest {

  @JvmField
  @Rule
  val tempFolder = TemporaryFolder()

  //Placeholders
  private companion object {
    val modifiers = Modifiers(12345)
    val methodDescriptor = "()V"
    val methodName = "name"

    val classReference = ClassReference("a/b/C")
    val methodReference = MethodReference(classReference, methodName, methodDescriptor)

    val classLocation = ClassLocation("d/e/F", "", modifiers)
    val classHierarchy = ClassHierarchy("class", false, ClassFileOrigin.IDE_CLASS, null, emptyList())

    val parameterNames = listOf("a", "b", "c")

    val signature = ""
    val methodLocation = MethodLocation(
        classLocation,
        methodName,
        methodDescriptor,
        parameterNames,
        signature,
        modifiers
    )
    val methodNotFoundProblem = MethodNotFoundProblem(
        methodReference,
        methodLocation,
        Instruction.INVOKE_VIRTUAL,
        classHierarchy
    )

    val pluginStructureError = PluginStructureError("error")
    val pluginStructureWarning = PluginStructureWarning("warning")

    val pluginDependency = PluginDependencyImpl("id", false, false)

    val dependencyNode = DependencyNode("pluginId", "version", listOf(
        MissingDependency(pluginDependency, "reason")
    ))

    val dependencyEdge = DependencyEdge(dependencyNode, dependencyNode, pluginDependency)
    val dependenciesGraph = DependenciesGraph(dependencyNode, listOf(dependencyNode), listOf(dependencyEdge))

    val ideVersion = IdeVersion.createIdeVersion("IU-181.1")
    val pluginInfo = CustomPluginInfo("id", "name", "version", "vendor", URL("http://download-url.com"), URL("http://browser-url.com"))
    val target = VerificationTarget.Ide(ideVersion)

    val compatibilityProblems = setOf(methodNotFoundProblem)
    val structureErrors = setOf(pluginStructureError)
    val structureWarnings = setOf(pluginStructureWarning)

    val deprecatedMethodUsage = DeprecatedMethodUsage(methodLocation, methodLocation, DeprecationInfo(true, "2018.1"))
    val deprecatedMethodUsages = setOf(deprecatedMethodUsage)

    val experimentalApiUsage = ExperimentalMethodUsage(methodLocation, methodLocation)
    val experimentalApiUsages = setOf(experimentalApiUsage)
  }

  @Test
  fun `test verification result can be serialized and deserialized`() {
    val result = VerificationResult.MissingDependencies()
    result.also {
      it.plugin = pluginInfo
      it.verificationTarget = target
      it.compatibilityProblems = compatibilityProblems
      it.pluginStructureErrors = structureErrors
      it.pluginStructureWarnings = structureWarnings
      it.dependenciesGraph = dependenciesGraph
      it.deprecatedUsages = deprecatedMethodUsages
      it.experimentalApiUsages = experimentalApiUsages
    }
    val file = tempFolder.newFile().toPath()
    VerificationResultPersistence.saveVerificationResult(result, file)
    val deserialized = VerificationResultPersistence.readVerificationResult(file)

    assertEquals(pluginInfo, deserialized.plugin)
    assertEquals(target, deserialized.verificationTarget)
    assertEquals(compatibilityProblems, deserialized.compatibilityProblems)
    assertEquals(structureErrors, deserialized.pluginStructureErrors)
    assertEquals(structureWarnings, deserialized.pluginStructureWarnings)
    assertEquals(dependenciesGraph, deserialized.dependenciesGraph)
    assertEquals(deprecatedMethodUsages, deserialized.deprecatedUsages)
    assertEquals(experimentalApiUsages, deserialized.experimentalApiUsages)
  }

}