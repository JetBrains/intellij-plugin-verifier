package com.jetbrains.pluginverifier.tests.filter

import com.jetbrains.pluginverifier.parameters.filtering.documented.DocClassRemoved
import com.jetbrains.pluginverifier.parameters.filtering.documented.DocMethodParameterTypeChanged
import com.jetbrains.pluginverifier.parameters.filtering.documented.DocMethodReturnTypeChanged
import com.jetbrains.pluginverifier.parameters.filtering.documented.DocPackageRemoved
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.modifiers.Modifiers.Modifier.*
import com.jetbrains.pluginverifier.results.problems.FieldNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.MethodNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.MethodNotImplementedProblem
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.tests.mocks.MOCK_METHOD_LOCATION
import com.jetbrains.pluginverifier.verifiers.logic.hierarchy.ClassHierarchyBuilder
import org.junit.Test

class DocumentedProblemsReportingTest : BaseDocumentedProblemsReportingTest() {

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
        ClassHierarchyBuilder.JAVA_LANG_OBJECT_HIERARCHY
    )

    //field with deleted owner
    val fieldWithRemovedOwnerProblem = FieldNotFoundProblem(
        FieldReference(deletedClassRef, "x", "I"),
        MOCK_METHOD_LOCATION,
        ClassHierarchyBuilder.JAVA_LANG_OBJECT_HIERARCHY,
        Instruction.GET_FIELD
    )

    val unrelatedClassRef = ClassReference("org/just/some/Class")

    //method with deleted param type
    val methodWithRemovedClassInSignature = MethodNotFoundProblem(
        MethodReference(unrelatedClassRef, "foo", "(Lorg/some/deleted/Class;)V"),
        MOCK_METHOD_LOCATION,
        Instruction.INVOKE_VIRTUAL,
        ClassHierarchyBuilder.JAVA_LANG_OBJECT_HIERARCHY
    )

    //field with deleted param type
    val fieldWithRemovedClassInType = FieldNotFoundProblem(
        FieldReference(unrelatedClassRef, "x", "Lorg/some/deleted/Class;"),
        MOCK_METHOD_LOCATION,
        ClassHierarchyBuilder.JAVA_LANG_OBJECT_HIERARCHY,
        Instruction.GET_FIELD
    )

    val methodWithOwnerFromRemovedPackage = with(methodWithRemovedOwnerProblem) {
      MethodNotFoundProblem(
          unresolvedMethod.copy(hostClass = ClassReference("some/removed/package/Class")),
          caller,
          instruction,
          methodOwnerHierarchy
      )
    }

    val fieldWithOwnerFromRemovedPackage = with(fieldWithRemovedOwnerProblem) {
      FieldNotFoundProblem(
          unresolvedField.copy(hostClass = ClassReference("some/removed/package/Class")),
          accessor,
          fieldOwnerHierarchy,
          instruction
      )
    }

    val docClassRemoved = DocClassRemoved("org/some/deleted/Class")
    val docPackageRemoved = DocPackageRemoved("some/removed/package")
    val problemToDocumentation = listOf(
        methodWithRemovedOwnerProblem to docClassRemoved,
        fieldWithRemovedOwnerProblem to docClassRemoved,

        methodWithRemovedClassInSignature to docClassRemoved,
        fieldWithRemovedClassInType to docClassRemoved,

        methodWithOwnerFromRemovedPackage to docPackageRemoved,
        fieldWithOwnerFromRemovedPackage to docPackageRemoved
    )

    assertProblemsDocumented(problemToDocumentation, createSimpleVerificationContext())
  }

  /**
   * Asserts that documentations 'method parameter changed' or 'method return type changed'
   * cover 'abstract method added' problems.
   */
  @Test
  fun `abstract method added problem is covered by method-return-type-change or method-parameter-type-changed documentations`() {
    val libInterfaceName = "org/lib/Interface"
    val clientImplName = "client/Implementation"
    val methodName = "foo"

    val libInterface = ClassLocation(libInterfaceName, "", Modifiers.of(PUBLIC, INTERFACE, ABSTRACT))
    val clientImplementation = ClassLocation(clientImplName, "", Modifiers.of(PUBLIC))

    val methodNotImplementedProblem = MethodNotImplementedProblem(
        MethodLocation(
            libInterface,
            methodName,
            "()V",
            emptyList(),
            "",
            Modifiers.of(PUBLIC, ABSTRACT)
        ),
        clientImplementation
    )

    assertProblemsDocumented(
        listOf(
            methodNotImplementedProblem to DocMethodReturnTypeChanged(libInterfaceName, methodName),
            methodNotImplementedProblem to DocMethodParameterTypeChanged(libInterfaceName, methodName)
        ),
        createSimpleVerificationContext()
    )
  }
}