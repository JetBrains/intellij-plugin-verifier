package com.jetbrains.pluginverifier.tests.filter

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.FixedClassesResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.pluginverifier.filtering.documented.*
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
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.modifier.MethodManifestation
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.implementation.ExceptionMethod
import org.junit.Test
import org.objectweb.asm.tree.ClassNode

class HierarchicalProblemsReportingTest : BaseDocumentedProblemsReportingTest() {

  private object SomeFileOrigin : FileOrigin {
    override val parent: FileOrigin? = null
  }

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
    val classALocation = ClassLocation("org/test/A", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)
    val classBLocation = ClassLocation("org/test/other/B", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)
    val classCLocation = ClassLocation("org/test/third/C", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)

    val classBReference = ClassReference("org/test/other/B")

    val methodLocation = MethodLocation(
      ClassLocation("SomeClass", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin),
      "someMethod",
      "()V",
      emptyList(),
      null,
      Modifiers.of(Modifiers.Modifier.PUBLIC)
    )

    val methodFooIsNotFoundProblem = MethodNotFoundProblem(
      MethodReference(classBReference, "foo", "()V"),
      methodLocation,
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
        Modifiers.of(Modifiers.Modifier.PUBLIC)
      ),
      AccessType.PRIVATE,
      methodLocation,
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
      methodLocation,
      JAVA_LANG_OBJECT_HIERARCHY,
      Instruction.GET_FIELD
    )

    val abstractMethodLocation = MethodLocation(
      ClassLocation("org/test/I", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin),
      "abstractMethod",
      "()V",
      emptyList(),
      null,
      Modifiers.of(Modifiers.Modifier.PUBLIC)
    )
    val incompleteClass = ClassLocation("org/test/IImplDerived", null, Modifiers.of(Modifiers.Modifier.PUBLIC), SomeFileOrigin)

    val methodNotImplementedProblem = MethodNotImplementedProblem(
      abstractMethodLocation,
      incompleteClass
    )

    val classNotFoundProblem = ClassNotFoundProblem(
      ClassReference("org/test/some/Inner\$Class"),
      methodLocation
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
      classCLocation
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

  private fun createVerificationContextForHierarchicalTest(): VerificationContext {
    val classes = buildClassesForHierarchicalTest()
    for (classNode in classes) {
      val byteCode = classNode.printBytecode()
      println(classNode.name)
      println(byteCode)
    }
    return createSimpleVerificationContext(FixedClassesResolver.create(classes, SomeFileOrigin, readMode = Resolver.ReadMode.FULL))
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
   * }
   *
   * package org.test.third;
   * public class C extends B {
   *   @Override public void finalMethod() { } //invalid overriding
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
      .make()

    val classCDescriptor = ByteBuddy()
      .subclass(classBDescriptor.typeDescription)
      .name("org.test.third.C")
      .defineMethod("finalMethod", Void.TYPE, Visibility.PUBLIC)
      .intercept(ExceptionMethod.throwing(RuntimeException::class.java))
      .make()

    return listOf(
      classADescriptor,
      classBDescriptor,
      classCDescriptor,
      interfaceIDescriptor,
      interfaceImplDescriptor,
      interfaceImplDerived
    ).map { it.bytes.createClassNode() }
  }


}