package com.jetbrains.pluginverifier.tests.filter

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.classes.resolvers.FixedClassesResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.core.VerificationResultHolder
import com.jetbrains.pluginverifier.parameters.filtering.DocumentedProblemsFilter
import com.jetbrains.pluginverifier.parameters.filtering.documented.*
import com.jetbrains.pluginverifier.reporting.verification.EmptyPluginVerificationReportage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.location.classpath.ClassPath
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.problems.FieldNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.MethodNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.MethodNotImplementedProblem
import com.jetbrains.pluginverifier.results.problems.Problem
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.tests.bytecode.createClassNode
import com.jetbrains.pluginverifier.tests.mocks.MOCK_METHOD_LOCATION
import com.jetbrains.pluginverifier.tests.mocks.MockIdePlugin
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.logic.hierarchy.ClassHierarchyBuilder
import net.bytebuddy.ByteBuddy
import org.hamcrest.collection.IsIn.isIn
import org.junit.Assert.assertThat
import org.junit.Test
import org.objectweb.asm.tree.ClassNode

class DocumentedProblemsReportingTest {

  /**
   * Check that the problem reported for the subtype B is ignored
   * because this problem is documented with respect to its superclass.
   */
  @Test
  fun `hierarchical problems should not be reported`() {
    val problemAndItsDocumentation = createProblemAndItsDocumentationTestMap()

    val problemsFilter = DocumentedProblemsFilter(problemAndItsDocumentation.values.toList())
    val verificationContext = createVerificationContextForHierarchicalTest(problemsFilter)
    problemAndItsDocumentation.keys.forEach { verificationContext.registerProblem(it) }

    val actualIgnoredProblems = verificationContext.resultHolder.ignoredProblems
    problemAndItsDocumentation.keys.forEach { problem -> assertThat(problem, isIn(actualIgnoredProblems)) }
  }

  private fun createProblemAndItsDocumentationTestMap(): Map<Problem, DocumentedProblem> {
    val classBReference = ClassReference("org/test/other/B")
    val methodFooIsNotFoundProblem = MethodNotFoundProblem(
        MethodReference(classBReference, "foo", "()V"),
        MOCK_METHOD_LOCATION,
        Instruction.INVOKE_VIRTUAL,
        ClassHierarchyBuilder.JAVA_LANG_OBJECT_HIERARCHY,
        IdeVersion.createIdeVersion("IU-163")
    )

    val fieldXNotFoundProblem = FieldNotFoundProblem(
        FieldReference(classBReference, "x", "I"),
        MOCK_METHOD_LOCATION,
        ClassHierarchyBuilder.JAVA_LANG_OBJECT_HIERARCHY,
        Instruction.GET_FIELD,
        IdeVersion.createIdeVersion("IU-163")
    )

    val abstractMethodLocation = MethodLocation(
        ClassLocation("org/test/I", "", ClassPath(ClassPath.Type.ROOT, ""), Modifiers(0x1)),
        "abstractMethod",
        "()V",
        emptyList(),
        "",
        Modifiers(0x1)
    )
    val incompleteClass = ClassLocation("org/test/IImplDerived", "", ClassPath(ClassPath.Type.ROOT, ""), Modifiers(0x1))

    val methodNotImplementedProblem = MethodNotImplementedProblem(
        abstractMethodLocation,
        incompleteClass
    )

    return mapOf(
        methodFooIsNotFoundProblem to DocMethodRemoved("org/test/A", "foo"),

        methodFooIsNotFoundProblem to DocMethodReturnTypeChanged("org/test/A", "foo"),

        methodFooIsNotFoundProblem to DocMethodParameterTypeChanged("org/test/A", "foo"),

        fieldXNotFoundProblem to DocFieldRemoved("org/test/A", "x"),

        fieldXNotFoundProblem to DocFieldTypeChanged("org/test/A", "x"),

        methodNotImplementedProblem to DocAbstractMethodAdded("org/test/IImpl", "abstractMethod")
    )
  }

  private fun createVerificationContextForHierarchicalTest(documentedProblemsFilter: DocumentedProblemsFilter): VerificationContext {
    val classes = buildClassesForHierarchicalTest()

    val idePlugin = MockIdePlugin(
        pluginId = "pluginId",
        pluginVersion = "1.0"
    )

    val ideVersion = IdeVersion.createIdeVersion("IU-163.1")

    return VerificationContext(
        idePlugin,
        ideVersion,
        FixedClassesResolver.create(classes),
        EmptyResolver,
        VerificationResultHolder(EmptyPluginVerificationReportage),
        emptyList(),
        false,
        listOf(documentedProblemsFilter)
    )
  }


  /**
   * public interface I {
   *
   * }
   *
   * public class IImpl implements I {
   *
   * }
   *
   * public class IImplDerived extends IImpl {
   *
   * }
   *
   * public class A {
   *
   * }
   *
   * public class B extends A {
   *
   * }
   */
  private fun buildClassesForHierarchicalTest(): List<ClassNode> {
    val interfaceIDescriptor = ByteBuddy()
        .makeInterface()
        .name("org.test.I")
        .make()

    val interfaceImplDescriptor = ByteBuddy()
        .subclass(interfaceIDescriptor.typeDescription)
        .name("org.test.IImpl")
        .make()

    val interfaceImplDerived = ByteBuddy()
        .subclass(interfaceImplDescriptor.typeDescription)
        .name("org.test.IImplDerived")
        .make()

    val classADescriptor = ByteBuddy()
        .subclass(Any::class.java)
        .name("org.test.A")
        .make()

    val classBDescriptor = ByteBuddy()
        .subclass(classADescriptor.typeDescription)
        .name("org.test.other.B")
        .make()

    return listOf(classADescriptor, classBDescriptor, interfaceIDescriptor, interfaceImplDescriptor, interfaceImplDerived).map {
      it.bytes.createClassNode()
    }
  }
}