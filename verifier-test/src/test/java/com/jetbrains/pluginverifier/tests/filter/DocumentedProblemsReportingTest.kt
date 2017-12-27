package com.jetbrains.pluginverifier.tests.filter

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.classes.resolvers.FixedClassesResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.core.VerificationResultHolder
import com.jetbrains.pluginverifier.parameters.filtering.DocumentedProblemsFilter
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.parameters.filtering.documented.*
import com.jetbrains.pluginverifier.reporting.verification.EmptyPluginVerificationReportage
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.location.classpath.ClassPath
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.problems.*
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

/**
 * This test asserts that the
 * [documented] [com.jetbrains.pluginverifier.parameters.filtering.DocumentedProblemsFilter]
 * problems are indeed excluded from the verification reports.
 */
class DocumentedProblemsReportingTest {

  /**
   * Check that the problem reported for the subtype B is ignored
   * because this problem is documented with respect to its superclass.
   */
  @Test
  fun `hierarchical problems should not be reported`() {
    val problemAndItsDocumentation = createProblemAndItsDocumentationTestMap()
    verify(problemAndItsDocumentation) {
      createVerificationContextForHierarchicalTest(it)
    }
  }

  private fun verify(problemAndItsDocumentation: Map<Problem, DocumentedProblem>,
                     contextProvider: (ProblemsFilter) -> VerificationContext) {
    val problems = problemAndItsDocumentation.keys
    val documentedProblems = problemAndItsDocumentation.values.toList()

    val problemsFilter = DocumentedProblemsFilter(documentedProblems)
    val verificationContext = contextProvider(problemsFilter)

    problems.forEach { verificationContext.registerProblem(it) }
    val actualIgnoredProblems = verificationContext.resultHolder.ignoredProblemsHolder.ignoredProblems
    problems.forEach { problem -> assertThat(problem, isIn(actualIgnoredProblems)) }
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

    val classNotFoundProblem = ClassNotFoundProblem(
        ClassReference("org/test/some/Inner\$Class"),
        MOCK_METHOD_LOCATION
    )

    return mapOf(
        methodFooIsNotFoundProblem to DocMethodRemoved("org/test/A", "foo"),

        methodFooIsNotFoundProblem to DocMethodReturnTypeChanged("org/test/A", "foo"),

        methodFooIsNotFoundProblem to DocMethodParameterTypeChanged("org/test/A", "foo"),

        fieldXNotFoundProblem to DocFieldRemoved("org/test/A", "x"),

        fieldXNotFoundProblem to DocFieldTypeChanged("org/test/A", "x"),

        methodNotImplementedProblem to DocAbstractMethodAdded("org/test/IImpl", "abstractMethod"),

        classNotFoundProblem to DocClassRemoved("org/test/some/Inner\$Class")
    )
  }

  private fun createVerificationContextForHierarchicalTest(problemsFilter: ProblemsFilter): VerificationContext {
    val classes = buildClassesForHierarchicalTest()
    return createSimpleVerificationContext(problemsFilter).copy(
        classLoader = FixedClassesResolver.create(classes)
    )
  }

  private fun createSimpleVerificationContext(problemsFilter: ProblemsFilter): VerificationContext {
    val idePlugin = MockIdePlugin(
        pluginId = "pluginId",
        pluginVersion = "1.0"
    )

    val ideVersion = IdeVersion.createIdeVersion("IU-163.1")

    return VerificationContext(
        idePlugin,
        ideVersion,
        EmptyResolver,
        EmptyResolver,
        VerificationResultHolder(EmptyPluginVerificationReportage),
        emptyList(),
        false,
        listOf(problemsFilter)
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

  /**
   * Asserts that
   * - `org.example.Class class removed` documentation covers
   *  the case of `... unresolved method com.Holder.foo(org.example.Class) : void`
   *
   * - `org.example.Class class removed` documentation covers
   *  the case of `... unresolved field com.Holder.x : org.example.Class`.
   *
   *  - `org.example package removed` documentation covers
   *  the case of `... unresolved method org.example.Class.foo() : void`.
   *
   *  - `org.example package removed` documentation covers
   *  the case of `... unresolved field org.example.Class.x : int`.
   *
   * - etc...
   */
  @Test
  fun `documented deletion of a class excludes unresolved methods and fields problems`() {
    val deletedClassRef = ClassReference("org/some/deleted/Class")

    //method with deleted owner
    val methodWithRemovedOwnerProblem = MethodNotFoundProblem(
        MethodReference(deletedClassRef, "foo", "()V"),
        MOCK_METHOD_LOCATION,
        Instruction.INVOKE_VIRTUAL,
        ClassHierarchyBuilder.JAVA_LANG_OBJECT_HIERARCHY,
        IdeVersion.createIdeVersion("IU-163")
    )

    //field with deleted owner
    val fieldWithRemovedOwnerProblem = FieldNotFoundProblem(
        FieldReference(deletedClassRef, "x", "I"),
        MOCK_METHOD_LOCATION,
        ClassHierarchyBuilder.JAVA_LANG_OBJECT_HIERARCHY,
        Instruction.GET_FIELD,
        IdeVersion.createIdeVersion("IU-163")
    )

    val unrelatedClassRef = ClassReference("org/just/some/Class")

    //method with deleted param type
    val methodWithRemovedClassInSignature = MethodNotFoundProblem(
        MethodReference(unrelatedClassRef, "foo", "(Lorg/some/deleted/Class;)V"),
        MOCK_METHOD_LOCATION,
        Instruction.INVOKE_VIRTUAL,
        ClassHierarchyBuilder.JAVA_LANG_OBJECT_HIERARCHY,
        IdeVersion.createIdeVersion("IU-163")
    )

    //field with deleted param type
    val fieldWithRemovedClassInType = FieldNotFoundProblem(
        FieldReference(unrelatedClassRef, "x", "Lorg/some/deleted/Class;"),
        MOCK_METHOD_LOCATION,
        ClassHierarchyBuilder.JAVA_LANG_OBJECT_HIERARCHY,
        Instruction.GET_FIELD,
        IdeVersion.createIdeVersion("IU-163")
    )

    val methodWithOwnerFromRemovedPackage = with(methodWithRemovedOwnerProblem) {
      copy(
          unresolvedMethod = unresolvedMethod.copy(
              unresolvedMethod.hostClass.copy(
                  className = "some/removed/package/Class"
              )
          )
      )
    }

    val fieldWithOwnerFromRemovedPackage = with(fieldWithRemovedOwnerProblem) {
      copy(
          unresolvedField = unresolvedField.copy(
              unresolvedField.hostClass.copy(
                  className = "some/removed/package/Class"
              )
          )
      )
    }

    val docClassRemoved = DocClassRemoved("org/some/deleted/Class")
    val docPackageRemoved = DocPackageRemoved("some/removed/package")
    val problemToDocumentation = mapOf(
        methodWithRemovedOwnerProblem to docClassRemoved,
        fieldWithRemovedOwnerProblem to docClassRemoved,

        methodWithRemovedClassInSignature to docClassRemoved,
        fieldWithRemovedClassInType to docClassRemoved,

        methodWithOwnerFromRemovedPackage to docPackageRemoved,
        fieldWithOwnerFromRemovedPackage to docPackageRemoved
    )

    verify(problemToDocumentation) {
      createSimpleVerificationContext(it)
    }
  }
}