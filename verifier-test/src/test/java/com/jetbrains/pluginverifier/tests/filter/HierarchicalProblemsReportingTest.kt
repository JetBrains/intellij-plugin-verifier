package com.jetbrains.pluginverifier.tests.filter

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.classes.resolvers.FixedClassesResolver
import com.jetbrains.pluginverifier.parameters.filtering.documented.*
import com.jetbrains.pluginverifier.parameters.packages.PackageFilter
import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.problems.*
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.tests.bytecode.createClassNode
import com.jetbrains.pluginverifier.tests.mocks.MOCK_METHOD_LOCATION
import com.jetbrains.pluginverifier.tests.mocks.PUBLIC_MODIFIERS
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import com.jetbrains.pluginverifier.verifiers.logic.hierarchy.ClassHierarchyBuilder
import com.jetbrains.pluginverifier.verifiers.resolution.DefaultClsResolver
import net.bytebuddy.ByteBuddy
import org.junit.Test
import org.objectweb.asm.tree.ClassNode

class HierarchicalProblemsReportingTest : BaseDocumentedProblemsReportingTest() {

  /**
   * Check that the problem reported for the subtype B is ignored
   * because this problem is documented with respect to its superclass.
   */
  @Test
  fun `hierarchical problems should not be reported`() {
    val problemAndItsDocumentation = createProblemAndItsDocumentationTestMap()
    assertProblemsDocumented(problemAndItsDocumentation, createVerificationContextForHierarchicalTest())
  }

  private fun createProblemAndItsDocumentationTestMap(): List<Pair<CompatibilityProblem, DocumentedProblem>> {
    val classBReference = ClassReference("org/test/other/B")
    val methodFooIsNotFoundProblem = MethodNotFoundProblem(
        MethodReference(classBReference, "foo", "()V"),
        MOCK_METHOD_LOCATION,
        Instruction.INVOKE_VIRTUAL,
        ClassHierarchyBuilder.JAVA_LANG_OBJECT_HIERARCHY
    )

    val constructorIsNotFoundProblem = with(methodFooIsNotFoundProblem) {
      MethodNotFoundProblem(
          unresolvedMethod.copy(methodName = "<init>"),
          caller,
          instruction,
          methodOwnerHierarchy
      )
    }

    val illegalMethodAccessProblem = IllegalMethodAccessProblem(
        constructorIsNotFoundProblem.unresolvedMethod,
        MethodLocation(
            ClassLocation(
                "org/test/other/B",
                "",
                PUBLIC_MODIFIERS
            ),
            "foo",
            "()V",
            emptyList(),
            "",
            PUBLIC_MODIFIERS
        ),
        AccessType.PRIVATE,
        MOCK_METHOD_LOCATION,
        Instruction.INVOKE_SPECIAL
    )

    val illegalConstructorAccessProblem = with(illegalMethodAccessProblem) {
      IllegalMethodAccessProblem(
          bytecodeMethodReference,
          inaccessibleMethod.copy(methodName = "<init>"),
          methodAccessModifier,
          caller,
          instruction
      )
    }

    val fieldXNotFoundProblem = FieldNotFoundProblem(
        FieldReference(classBReference, "x", "I"),
        MOCK_METHOD_LOCATION,
        ClassHierarchyBuilder.JAVA_LANG_OBJECT_HIERARCHY,
        Instruction.GET_FIELD
    )

    val abstractMethodLocation = MethodLocation(
        ClassLocation("org/test/I", "", PUBLIC_MODIFIERS),
        "abstractMethod",
        "()V",
        emptyList(),
        "",
        PUBLIC_MODIFIERS
    )
    val incompleteClass = ClassLocation("org/test/IImplDerived", "", PUBLIC_MODIFIERS)

    val methodNotImplementedProblem = MethodNotImplementedProblem(
        abstractMethodLocation,
        incompleteClass
    )

    val classNotFoundProblem = ClassNotFoundProblem(
        ClassReference("org/test/some/Inner\$Class"),
        MOCK_METHOD_LOCATION
    )

    return listOf(
        methodFooIsNotFoundProblem to DocMethodRemoved("org/test/A", "foo"),

        methodFooIsNotFoundProblem to DocMethodReturnTypeChanged("org/test/A", "foo"),

        methodFooIsNotFoundProblem to DocMethodParameterTypeChanged("org/test/A", "foo"),

        fieldXNotFoundProblem to DocFieldRemoved("org/test/A", "x"),

        fieldXNotFoundProblem to DocFieldTypeChanged("org/test/A", "x"),

        methodNotImplementedProblem to DocAbstractMethodAdded("org/test/IImpl", "abstractMethod"),

        classNotFoundProblem to DocClassRemoved("org/test/some/Inner\$Class"),

        constructorIsNotFoundProblem to DocMethodRemoved("org/test/other/B", "<init>"),

        constructorIsNotFoundProblem to DocMethodParameterTypeChanged("org/test/other/B", "<init>"),

        illegalMethodAccessProblem to DocMethodVisibilityChanged("org/test/other/B", "foo"),

        illegalConstructorAccessProblem to DocMethodVisibilityChanged("org/test/other/B", "<init>")
    )
  }

  private fun createVerificationContextForHierarchicalTest(): VerificationContext {
    val classes = buildClassesForHierarchicalTest()
    return createSimpleVerificationContext().copy(
        clsResolver = DefaultClsResolver(
            FixedClassesResolver.create(classes),
            EmptyResolver,
            EmptyResolver,
            EmptyResolver,
            PackageFilter(emptyList()),
            emptyList()
        )
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

    return listOf(
        classADescriptor,
        classBDescriptor,
        interfaceIDescriptor,
        interfaceImplDescriptor,
        interfaceImplDerived
    ).map { it.bytes.createClassNode() }
  }


}