package com.jetbrains.pluginverifier.tests.filter

import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.classes.resolvers.FixedClassesResolver
import com.jetbrains.pluginverifier.parameters.filtering.documented.*
import com.jetbrains.pluginverifier.parameters.packages.PackageFilter
import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.results.instruction.Instruction
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.problems.*
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.results.reference.FieldReference
import com.jetbrains.pluginverifier.results.reference.MethodReference
import com.jetbrains.pluginverifier.tests.bytecode.createClassNode
import com.jetbrains.pluginverifier.tests.bytecode.printBytecode
import com.jetbrains.pluginverifier.tests.mocks.MOCK_METHOD_LOCATION
import com.jetbrains.pluginverifier.tests.mocks.MockClassFileOrigin
import com.jetbrains.pluginverifier.tests.mocks.PUBLIC_MODIFIERS
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.verifiers.resolution.IdePluginClassResolver
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.modifier.MethodManifestation
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.implementation.ExceptionMethod
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
    val classALocation = ClassLocation("org/test/A", null, PUBLIC_MODIFIERS, MockClassFileOrigin)
    val classBLocation = ClassLocation("org/test/other/B", null, PUBLIC_MODIFIERS, MockClassFileOrigin)

    val classBReference = ClassReference("org/test/other/B")

    val methodFooIsNotFoundProblem = MethodNotFoundProblem(
        MethodReference(classBReference, "foo", "()V"),
        MOCK_METHOD_LOCATION,
        Instruction.INVOKE_VIRTUAL,
        JAVA_LANG_OBJECT_HIERARCHY
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
            classBLocation,
            "foo",
            "()V",
            emptyList(),
            null,
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
        JAVA_LANG_OBJECT_HIERARCHY,
        Instruction.GET_FIELD
    )

    val abstractMethodLocation = MethodLocation(
        ClassLocation("org/test/I", null, PUBLIC_MODIFIERS, MockClassFileOrigin),
        "abstractMethod",
        "()V",
        emptyList(),
        null,
        PUBLIC_MODIFIERS
    )
    val incompleteClass = ClassLocation("org/test/IImplDerived", null, PUBLIC_MODIFIERS, MockClassFileOrigin)

    val methodNotImplementedProblem = MethodNotImplementedProblem(
        abstractMethodLocation,
        incompleteClass
    )

    val classNotFoundProblem = ClassNotFoundProblem(
        ClassReference("org/test/some/Inner\$Class"),
        MOCK_METHOD_LOCATION
    )

    val overridingFinalMethodProblem = OverridingFinalMethodProblem(
        MethodLocation(
            classALocation,
            "finalMethod",
            "()V",
            emptyList(),
            null,
            Modifiers.of(Modifiers.Modifier.PUBLIC, Modifiers.Modifier.FINAL)
        ),
        classBLocation
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

        illegalConstructorAccessProblem to DocMethodVisibilityChanged("org/test/other/B", "<init>"),

        overridingFinalMethodProblem to DocMethodMarkedFinal("org/test/other/A", "finalMethod"),

        overridingFinalMethodProblem to DocFinalMethodInherited("org/test/other/B", "org/test/A", "finalMethod")
    )
  }

  private fun createVerificationContextForHierarchicalTest(): PluginVerificationContext {
    val classes = buildClassesForHierarchicalTest()
    for (classNode in classes) {
      val byteCode = classNode.printBytecode()
      println(classNode.name)
      println(byteCode)
    }
    return createSimpleVerificationContext().copy(
        classResolver = IdePluginClassResolver(
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
   * package org.test;
   * public interface I {
   *
   * }
   *
   * package org.test;
   * public class IImpl implements I {
   *
   * }
   *
   * package org.test;
   * public class IImplDerived extends IImpl {
   *
   * }
   *
   * package org.test;
   * public class A {
   *   final public void finalMethod() { }
   * }
   *
   * package org.test.other;
   * public class B extends A {
   *   @Override public void finalMethod() { } //invalid overriding
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
        .defineMethod("finalMethod", Void.TYPE, Visibility.PUBLIC, MethodManifestation.FINAL)
        .intercept(ExceptionMethod.throwing(RuntimeException::class.java))
        .make()

    val classBDescriptor = ByteBuddy()
        .subclass(classADescriptor.typeDescription)
        .name("org.test.other.B")
        .defineMethod("finalMethod", Void.TYPE, Visibility.PUBLIC)
        .intercept(ExceptionMethod.throwing(RuntimeException::class.java))
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