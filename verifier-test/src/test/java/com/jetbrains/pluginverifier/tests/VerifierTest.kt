package com.jetbrains.pluginverifier.tests

import com.intellij.structure.impl.domain.PluginDependencyImpl
import com.jetbrains.pluginverifier.api.Verdict
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.location.*
import com.jetbrains.pluginverifier.problems.*
import com.jetbrains.pluginverifier.reference.ClassReference
import com.jetbrains.pluginverifier.reference.SymbolicReference
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class VerifierTest {

  companion object {
    lateinit var result: Verdict.MissingDependencies

    lateinit var actualProblems: Set<Problem>

    lateinit var redundantProblems: MutableList<Problem>

    @BeforeClass
    @JvmStatic
    fun verifyMockPlugin() {
      prepareTestEnvironment()
      var ideaFile = File("build/mocks/after-idea")
      if (!ideaFile.exists()) {
        ideaFile = File("verifier-test/build/mocks/after-idea")
      }
      assertTrue(ideaFile.exists())
      var pluginFile = File("build/mocks/mock-plugin-1.0.jar")
      if (!pluginFile.exists()) {
        pluginFile = File("verifier-test/build/mocks/mock-plugin-1.0.jar")
      }
      assertTrue(pluginFile.exists())
      val verificationResults = ResultBuilder.doIdeaAndPluginVerification(ideaFile, pluginFile)
      assertTrue(verificationResults.size == 1)
      assertTrue(verificationResults[0].verdict is Verdict.MissingDependencies)
      result = verificationResults[0].verdict as Verdict.MissingDependencies
      actualProblems = result.problems
      redundantProblems = actualProblems.toMutableList()
    }

    private fun prepareTestEnvironment() {
      System.setProperty("plugin.verifier.test.mode", "true")
      System.setProperty("plugin.verifier.test.private.interface.method.name", "privateInterfaceMethodTestName")
    }

    @AfterClass
    @JvmStatic
    fun assertNoRedundantProblems() {
      val message = redundantProblems.joinToString(separator = "\n") { "${it.getShortDescription()}:\n    ${it.getFullDescription()}" }
      assertTrue("Redundant problems: \n$message", redundantProblems.isEmpty())
    }

    val PUBLIC_CLASS_AF = AccessFlags(0x21)
    val PUBLIC_METHOD_AF = AccessFlags(0x1)
    val PUBLIC_INTERFACE_AF = AccessFlags(0x601)
    val PUBLIC_ABSTRACT_METHOD_AF = AccessFlags(0x401)
    val PUBLIC_ABSTRACT_CLASS_AF = AccessFlags(0x421)

    val PLUGIN_CLASS_PATH = ClassPath(ClassPath.Type.ROOT, "mock-plugin-1.0.jar")
    val IDEA_CLASS_PATH = ClassPath(ClassPath.Type.JAR_FILE, "after-idea-1.0.jar")

    fun pluginMethod(
        hostClass: ClassLocation,
        methodName: String,
        methodDescriptor: String,
        parameterNames: List<String>,
        signature: String?,
        accessFlags: AccessFlags
    ): MethodLocation = Location.fromMethod(hostClass, methodName, methodDescriptor, parameterNames, signature, accessFlags)

    fun pluginClass(className: String, signature: String?, accessFlags: AccessFlags): ClassLocation = Location.fromClass(className, signature, PLUGIN_CLASS_PATH, accessFlags)

    fun pluginField(
        hostClass: ClassLocation,
        fieldName: String,
        fieldDescriptor: String,
        signature: String?,
        accessFlags: AccessFlags
    ): FieldLocation = Location.fromField(hostClass, fieldName, fieldDescriptor, signature, accessFlags)
  }

  @Test
  fun checkMissingDeps() {
    val missingDependencies = result.missingDependencies
    assertFalse(missingDependencies.isEmpty())
    println(missingDependencies)
    val expectedDep = MissingDependency(PluginDependencyImpl("MissingPlugin", true), false, "Plugin MissingPlugin doesn't have a build compatible with IU-145.500")
    assertTrue(expectedDep in missingDependencies)
  }

  private fun assertProblemFound(problem: Problem, expectedFullDescription: String, expectedShortDescription: String) {
    assertTrue("${problem.getShortDescription()} is not found", actualProblems.contains(problem))
    redundantProblems.remove(problem)
    assertEquals(expectedFullDescription, problem.getFullDescription())
    assertEquals(expectedShortDescription, problem.getShortDescription())
  }

  @Test
  fun notImplementedAbstractMethodFromInterface() {
    val notImplementedMethod = Location.fromMethod(
        Location.fromClass("com/intellij/openapi/components/PersistentStateComponent", "<T:Ljava/lang/Object;>Ljava/lang/Object;", IDEA_CLASS_PATH, PUBLIC_INTERFACE_AF),
        "getState",
        "()Ljava/lang/Object;",
        emptyList(),
        "()TT;",
        PUBLIC_ABSTRACT_METHOD_AF
    )
    val incompleteClass = pluginClass("mock/plugin/NotImplementedProblem", null, PUBLIC_CLASS_AF)
    val problem = MethodNotImplementedProblem(notImplementedMethod, incompleteClass)
    assertProblemFound(problem,
        "Non-abstract class mock.plugin.NotImplementedProblem inherits from com.intellij.openapi.components.PersistentStateComponent<T> but doesn't implement the abstract method getState() : T. This can lead to **AbstractMethodError** exception at runtime.",
        "Abstract method com.intellij.openapi.components.PersistentStateComponent<T>.getState() : T is not implemented"
    )
  }

  @Test
  fun notImplementedPrivateOverridingFromInterface() {
    val notImplementedMethod = Location.fromMethod(
        Location.fromClass("com/intellij/openapi/components/PersistentStateComponent", "<T:Ljava/lang/Object;>Ljava/lang/Object;", IDEA_CLASS_PATH, PUBLIC_INTERFACE_AF),
        "getState",
        "()Ljava/lang/Object;",
        emptyList(),
        "()TT;",
        PUBLIC_ABSTRACT_METHOD_AF
    )
    val incompleteClass = pluginClass("mock/plugin/private_and_static/PrivateOverridingNotImplemented", "Ljava/lang/Object;Lcom/intellij/openapi/components/PersistentStateComponent<Ljava/lang/String;>;", PUBLIC_CLASS_AF)
    val problem = MethodNotImplementedProblem(notImplementedMethod, incompleteClass)
    assertProblemFound(problem, "Non-abstract class mock.plugin.private_and_static.PrivateOverridingNotImplemented inherits from com.intellij.openapi.components.PersistentStateComponent<T> but doesn't implement the abstract method getState() : T. This can lead to **AbstractMethodError** exception at runtime.",
        "Abstract method com.intellij.openapi.components.PersistentStateComponent<T>.getState() : T is not implemented"
    )
  }

  @Test
  fun notImplementedStaticOverridingFromInterface() {
    val notImplementedMethod = Location.fromMethod(
        Location.fromClass("com/intellij/openapi/components/PersistentStateComponent", "<T:Ljava/lang/Object;>Ljava/lang/Object;", IDEA_CLASS_PATH, PUBLIC_INTERFACE_AF),
        "getState",
        "()Ljava/lang/Object;",
        emptyList(),
        "()TT;",
        PUBLIC_ABSTRACT_METHOD_AF
    )
    val incompleteClass = pluginClass("mock/plugin/private_and_static/StaticOverridingNotImplemented", "Ljava/lang/Object;Lcom/intellij/openapi/components/PersistentStateComponent<Ljava/lang/String;>;", PUBLIC_CLASS_AF)
    val problem = MethodNotImplementedProblem(notImplementedMethod, incompleteClass)
    assertProblemFound(problem, "Non-abstract class mock.plugin.private_and_static.StaticOverridingNotImplemented inherits from com.intellij.openapi.components.PersistentStateComponent<T> but doesn't implement the abstract method getState() : T. This can lead to **AbstractMethodError** exception at runtime.",
        "Abstract method com.intellij.openapi.components.PersistentStateComponent<T>.getState() : T is not implemented"
    )
  }


  @Test
  fun notImplementedAbstractMethodFromAbstractClass() {
    val notImplementedMethod = Location.fromMethod(
        Location.fromClass("com/intellij/psi/search/UseScopeEnlarger", null, IDEA_CLASS_PATH, PUBLIC_ABSTRACT_CLASS_AF),
        "getAdditionalUseScope",
        "(Lcom/intellij/psi/PsiElement;)Lcom/intellij/psi/search/SearchScope;",
        listOf("arg0"),
        null,
        PUBLIC_ABSTRACT_METHOD_AF
    )
    val incompleteClass = pluginClass("mock/plugin/abstrackt/NotImplementedAbstractMethod", null, PUBLIC_CLASS_AF)
    val problem = MethodNotImplementedProblem(notImplementedMethod, incompleteClass)
    assertProblemFound(problem,
        "Non-abstract class mock.plugin.abstrackt.NotImplementedAbstractMethod inherits from com.intellij.psi.search.UseScopeEnlarger but doesn't implement the abstract method getAdditionalUseScope(PsiElement arg0) : SearchScope. This can lead to **AbstractMethodError** exception at runtime.",
        "Abstract method com.intellij.psi.search.UseScopeEnlarger.getAdditionalUseScope(PsiElement arg0) : SearchScope is not implemented"
    )
  }


  @Test
  fun overridingFinalMethod() {
    val finalMethod = Location.fromMethod(
        Location.fromClass("com/intellij/openapi/actionSystem/AnAction", null, IDEA_CLASS_PATH, PUBLIC_ABSTRACT_CLASS_AF),
        "isEnabledInModalContext",
        "()Z",
        emptyList(),
        null,
        AccessFlags(0x11)
    )
    val overridingClass = pluginClass("mock/plugin/OverrideFinalMethodProblem", null, PUBLIC_CLASS_AF)
    val problem = OverridingFinalMethodProblem(finalMethod, overridingClass)
    assertProblemFound(problem,
        "Class mock.plugin.OverrideFinalMethodProblem overrides the final method com.intellij.openapi.actionSystem.AnAction.isEnabledInModalContext() : boolean. This can lead to **VerifyError** exception at runtime.",
        "Overriding a final method com.intellij.openapi.actionSystem.AnAction.isEnabledInModalContext() : boolean"
    )
  }

  @Test
  fun staticAccessOfNonStaticField() {
    val accessor = pluginMethod(pluginClass("mock/plugin/field/FieldProblemsContainer", null, PUBLIC_CLASS_AF), "staticAccessOnInstance", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    val problem = StaticAccessOfNonStaticFieldProblem(
        Location.fromField(
            Location.fromClass("fields/FieldsContainer", null, IDEA_CLASS_PATH, PUBLIC_CLASS_AF),
            "instanceField",
            "I",
            null,
            PUBLIC_METHOD_AF
        ), accessor, Instruction.GET_STATIC
    )
    assertProblemFound(problem,
        "Method mock.plugin.field.FieldProblemsContainer.staticAccessOnInstance() : void has static access instruction *getstatic* referencing a non-static field fields.FieldsContainer.instanceField : int. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Attempt to execute a static access instruction *getstatic* on a non-static field fields.FieldsContainer.instanceField : int"
    )
  }

  @Test
  fun changeFinalNonStaticField() {
    val accessor = pluginMethod(pluginClass("mock/plugin/field/FieldProblemsContainer", null, PUBLIC_CLASS_AF), "setOnFinalFieldFromNotInitMethod", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    val problem = ChangeFinalFieldProblem(
        Location.fromField(
            Location.fromClass("fields/FieldsContainer", null, IDEA_CLASS_PATH, PUBLIC_CLASS_AF),
            "finalField",
            "I",
            null,
            AccessFlags(0x11)
        ), accessor, Instruction.PUT_FIELD
    )
    assertProblemFound(problem,
        "Method mock.plugin.field.FieldProblemsContainer.setOnFinalFieldFromNotInitMethod() : void has modifying instruction *putfield* referencing a final field fields.FieldsContainer.finalField : int. This can lead to **IllegalAccessError** exception at runtime.",
        "Attempt to change a final field fields.FieldsContainer.finalField : int"
    )
  }

  @Test
  fun changeFinalStaticField() {
    val accessor = pluginMethod(pluginClass("mock/plugin/field/FieldProblemsContainer", null, PUBLIC_CLASS_AF), "setOnStaticFinalFieldFromNotClinitMethod", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    val problem = ChangeFinalFieldProblem(
        Location.fromField(
            Location.fromClass("fields/FieldsContainer", null, IDEA_CLASS_PATH, PUBLIC_CLASS_AF),
            "staticFinalField",
            "I",
            null,
            AccessFlags(0x19)
        ),
        accessor,
        Instruction.PUT_STATIC
    )
    assertProblemFound(problem,
        "Method mock.plugin.field.FieldProblemsContainer.setOnStaticFinalFieldFromNotClinitMethod() : void has modifying instruction *putstatic* referencing a final field fields.FieldsContainer.staticFinalField : int. This can lead to **IllegalAccessError** exception at runtime.",
        "Attempt to change a final field fields.FieldsContainer.staticFinalField : int"
    )
  }

  @Test
  fun abstractClassInstantiation() {
    val creator = pluginMethod(pluginClass("mock/plugin/news/NewProblems", null, PUBLIC_CLASS_AF), "abstractClass", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    val problem = AbstractClassInstantiationProblem(
        Location.fromClass("misc/BecomeAbstract", null, IDEA_CLASS_PATH, PUBLIC_ABSTRACT_CLASS_AF),
        creator
    )
    assertProblemFound(problem,
        "Method mock.plugin.news.NewProblems.abstractClass() : void has instantiation *new* instruction referencing an abstract class misc.BecomeAbstract. This can lead to **InstantiationError** exception at runtime.",
        "Instantiation of an abstract class misc.BecomeAbstract"
    )
  }

  @Test
  fun interfaceInstantiation() {
    val creator = pluginMethod(pluginClass("mock/plugin/news/NewProblems", null, PUBLIC_CLASS_AF), "newInterface", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    val problem = InterfaceInstantiationProblem(
        Location.fromClass("misc/BecomeInterface", null, IDEA_CLASS_PATH, PUBLIC_INTERFACE_AF),
        creator
    )
    assertProblemFound(problem,
        "Method mock.plugin.news.NewProblems.newInterface() : void has instantiation *new* instruction referencing an interface misc.BecomeInterface. This can lead to **InstantiationError** exception at runtime.",
        "Instantiation of an interface misc.BecomeInterface"
    )

    val initOnInterfaceMethod = InvokeClassMethodOnInterfaceProblem(
        SymbolicReference.methodOf(
            "misc/BecomeInterface",
            "<init>",
            "()V"
        ),
        creator,
        Instruction.INVOKE_SPECIAL
    )
    assertProblemFound(initOnInterfaceMethod,
        "Method mock.plugin.news.NewProblems.newInterface() : void has invocation *invokespecial* instruction referencing a *class* method misc.BecomeInterface.<init>() : void, but the method's host misc.BecomeInterface is an *interface*. This can lead to **IncompatibleClassChangeError** at runtime.",
        "Incompatible change of class misc.BecomeInterface to interface"
    )
  }

  @Test
  fun nonStaticAccessOfStaticField() {
    val accessor = pluginMethod(pluginClass("mock/plugin/field/FieldProblemsContainer", null, PUBLIC_CLASS_AF), "instanceAccessOnStatic", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    val problem = NonStaticAccessOfStaticFieldProblem(
        Location.fromField(
            Location.fromClass("fields/FieldsContainer", null, IDEA_CLASS_PATH, PUBLIC_CLASS_AF),
            "staticField",
            "I",
            null,
            AccessFlags(0x9)
        ),
        accessor,
        Instruction.GET_FIELD
    )
    assertProblemFound(problem,
        "Method mock.plugin.field.FieldProblemsContainer.instanceAccessOnStatic() : void has non-static access instruction *getfield* referencing a static field fields.FieldsContainer.staticField : int. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Attempt to execute a non-static access instruction *getfield* on a static field fields.FieldsContainer.staticField : int"
    )
  }

  @Test
  fun superClassBecameInterface() {
    val problem = SuperClassBecameInterfaceProblem(
        pluginClass("mock/plugin/inheritance/SuperClassBecameInterface", null, PUBLIC_CLASS_AF),
        Location.fromClass("misc/BecomeInterface", null, IDEA_CLASS_PATH, AccessFlags(0x601))
    )
    assertProblemFound(problem,
        "Class mock.plugin.inheritance.SuperClassBecameInterface has a *super class* misc.BecomeInterface which is actually an *interface*. This can lead to **IncompatibleClassChangeError** at runtime.",
        "Incompatible change of super class misc.BecomeInterface to interface"
    )

    val initCaller = pluginMethod(
        pluginClass("mock/plugin/inheritance/SuperClassBecameInterface", null, PUBLIC_CLASS_AF),
        "<init>",
        "()V",
        emptyList(),
        null,
        PUBLIC_METHOD_AF
    )

    val initOnInterfaceMethod = InvokeClassMethodOnInterfaceProblem(
        SymbolicReference.methodOf(
            "misc/BecomeInterface",
            "<init>",
            "()V"
        ),
        initCaller,
        Instruction.INVOKE_SPECIAL
    )
    assertProblemFound(initOnInterfaceMethod,
        "Method mock.plugin.inheritance.SuperClassBecameInterface.<init>() : void has invocation *invokespecial* instruction referencing a *class* method misc.BecomeInterface.<init>() : void, but the method's host misc.BecomeInterface is an *interface*. This can lead to **IncompatibleClassChangeError** at runtime.",
        "Incompatible change of class misc.BecomeInterface to interface"
    )
  }

  @Test
  fun invokeClassMethodOnInterface() {
    val caller = pluginMethod(
        pluginClass("mock/plugin/invokeClassMethodOnInterface/Caller", null, PUBLIC_CLASS_AF),
        "call",
        "(Lmisc/BecomeInterface;)V",
        listOf("b"),
        null,
        PUBLIC_METHOD_AF
    )
    val problem = InvokeClassMethodOnInterfaceProblem(
        SymbolicReference.methodOf(
            "misc/BecomeInterface",
            "invokeVirtualMethod",
            "()V"
        ),
        caller,
        Instruction.INVOKE_VIRTUAL
    )
    assertProblemFound(problem,
        "Method mock.plugin.invokeClassMethodOnInterface.Caller.call(BecomeInterface b) : void has invocation *invokevirtual* instruction referencing a *class* method misc.BecomeInterface.invokeVirtualMethod() : void, but the method's host misc.BecomeInterface is an *interface*. This can lead to **IncompatibleClassChangeError** at runtime.",
        "Incompatible change of class misc.BecomeInterface to interface"
    )
  }

  @Test
  fun superInterfaceBecameClass() {
    val problem = SuperInterfaceBecameClassProblem(
        pluginClass("mock/plugin/inheritance/SuperInterfaceBecomeClass", null, PUBLIC_INTERFACE_AF),
        Location.fromClass("misc/BecomeClass", null, IDEA_CLASS_PATH, PUBLIC_CLASS_AF)
    )
    assertProblemFound(problem,
        "Interface mock.plugin.inheritance.SuperInterfaceBecomeClass has a *super interface* misc.BecomeClass which is actually a *class*. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Incompatible change of super interface misc.BecomeClass to class"
    )
  }

  @Test
  fun invokeInterfaceMethodOnClass() {
    val caller = pluginMethod(
        pluginClass("mock/plugin/invokeClassMethodOnInterface/Caller", null, PUBLIC_CLASS_AF),
        "call2",
        "(Lmisc/BecomeClass;)V",
        listOf("b"),
        null,
        PUBLIC_METHOD_AF
    )
    val problem = InvokeInterfaceMethodOnClassProblem(
        SymbolicReference.methodOf(
            "misc/BecomeClass",
            "invokeInterfaceOnClass",
            "()V"
        ),
        caller,
        Instruction.INVOKE_INTERFACE
    )
    assertProblemFound(problem,
        "Method mock.plugin.invokeClassMethodOnInterface.Caller.call2(BecomeClass b) : void has invocation *invokeinterface* instruction referencing an *interface* method misc.BecomeClass.invokeInterfaceOnClass() : void, but the method's host misc.BecomeClass is a *class*. This can lead to **IncompatibleClassChangeError** at runtime.",
        "Incompatible change of interface misc.BecomeClass to class"
    )
  }


  @Test
  fun inheritsFromFinalClass() {
    val pluginClass = pluginClass("mock/plugin/finals/InheritFromFinalClass", null, PUBLIC_CLASS_AF)
    val problem = InheritFromFinalClassProblem(
        pluginClass,
        Location.fromClass("finals/BecomeFinal", null, IDEA_CLASS_PATH, AccessFlags(0x31))
    )
    assertProblemFound(problem,
        "Class mock.plugin.finals.InheritFromFinalClass inherits from a final class finals.BecomeFinal. This can lead to **VerifyError** exception at runtime.",
        "Inheritance from a final class finals.BecomeFinal"
    )
  }

  @Test
  fun invokeStaticOnNonStaticMethod() {
    val problem = InvokeStaticOnNonStaticMethodProblem(
        Location.fromMethod(
            Location.fromClass("invocation/InvocationProblems", null, IDEA_CLASS_PATH, PUBLIC_CLASS_AF),
            "wasStatic",
            "()V",
            emptyList(),
            null,
            PUBLIC_METHOD_AF
        ),
        pluginMethod(pluginClass("mock/plugin/invokeStaticOnInstance/InvocationProblemsUser", null, PUBLIC_CLASS_AF), "foo", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    )
    assertProblemFound(problem,
        "Method mock.plugin.invokeStaticOnInstance.InvocationProblemsUser.foo() : void contains an *invokestatic* instruction referencing a non-static method invocation.InvocationProblems.wasStatic() : void. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Attempt to execute an *invokestatic* instruction on a non-static method invocation.InvocationProblems.wasStatic() : void"
    )
  }

  @Test
  fun invokeVirtualOnStaticMethod() {
    val problem = InvokeNonStaticInstructionOnStaticMethodProblem(
        Location.fromMethod(
            Location.fromClass("com/intellij/lang/SmartEnterProcessor", null, IDEA_CLASS_PATH, PUBLIC_ABSTRACT_CLASS_AF),
            "commit",
            "()V",
            emptyList(),
            null,
            AccessFlags(0x9)
        ),
        pluginMethod(pluginClass("mock/plugin/invokeVirtualOnStatic/SmartEnterProcessorUser", null, PUBLIC_CLASS_AF), "main", "()V", emptyList(), null, PUBLIC_METHOD_AF),
        Instruction.INVOKE_VIRTUAL
    )
    assertProblemFound(problem,
        "Method mock.plugin.invokeVirtualOnStatic.SmartEnterProcessorUser.main() : void contains an *invokevirtual* instruction referencing a static method com.intellij.lang.SmartEnterProcessor.commit() : void. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Attempt to execute a non-static instruction *invokevirtual* on a static method com.intellij.lang.SmartEnterProcessor.commit() : void"
    )
  }

  @Test
  fun invokeSpecialOnStaticMethod() {
    val problem = InvokeNonStaticInstructionOnStaticMethodProblem(
        Location.fromMethod(
            Location.fromClass("invokespecial/AbstractParent", null, IDEA_CLASS_PATH, PUBLIC_ABSTRACT_CLASS_AF),
            "becomeStatic",
            "()V",
            emptyList(),
            null,
            AccessFlags(0x9)
        ),
        pluginMethod(pluginClass("mock/plugin/invokespecial/Child", null, PUBLIC_ABSTRACT_CLASS_AF), "invokeSpecialOnStaticMethod", "()V", emptyList(), null, PUBLIC_METHOD_AF),
        Instruction.INVOKE_SPECIAL
    )
    assertProblemFound(problem,
        "Method mock.plugin.invokespecial.Child.invokeSpecialOnStaticMethod() : void contains an *invokespecial* instruction referencing a static method invokespecial.AbstractParent.becomeStatic() : void. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Attempt to execute a non-static instruction *invokespecial* on a static method invokespecial.AbstractParent.becomeStatic() : void"
    )
  }

  @Test
  fun invokeInterfaceOnStaticMethod() {
    val problem = InvokeNonStaticInstructionOnStaticMethodProblem(
        Location.fromMethod(
            Location.fromClass("statics/MethodBecameStatic", null, IDEA_CLASS_PATH, PUBLIC_INTERFACE_AF),
            "becomeStatic",
            "()V",
            emptyList(),
            null,
            AccessFlags(0x9)
        ),
        pluginMethod(pluginClass("mock/plugin/invokeClassMethodOnInterface/Caller", null, PUBLIC_CLASS_AF), "call3", "(Lstatics/MethodBecameStatic;)V", listOf("b"), null, PUBLIC_METHOD_AF),
        Instruction.INVOKE_INTERFACE
    )
    assertProblemFound(problem,
        "Method mock.plugin.invokeClassMethodOnInterface.Caller.call3(MethodBecameStatic b) : void contains an *invokeinterface* instruction referencing a static method statics.MethodBecameStatic.becomeStatic() : void. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Attempt to execute a non-static instruction *invokeinterface* on a static method statics.MethodBecameStatic.becomeStatic() : void"
    )
  }

  @Test
  fun abstractMethodInvocation() {
    val caller = pluginMethod(pluginClass("mock/plugin/invokespecial/Child", null, PUBLIC_ABSTRACT_CLASS_AF), "bar", "()V", emptyList(), null, PUBLIC_METHOD_AF)

    val problem = AbstractMethodInvocationProblem(
        Location.fromMethod(
            Location.fromClass("invokespecial/AbstractParent", null, IDEA_CLASS_PATH, PUBLIC_ABSTRACT_CLASS_AF),
            "foo",
            "()V",
            emptyList(),
            null,
            PUBLIC_ABSTRACT_METHOD_AF
        ),
        caller,
        Instruction.INVOKE_SPECIAL
    )
    assertProblemFound(problem,
        "Method mock.plugin.invokespecial.Child.bar() : void contains an *invokespecial* instruction referencing a method invokespecial.AbstractParent.foo() : void which doesn't have a non-abstract implementation. This can lead to **AbstractMethodError** exception at runtime.",
        "Attempt to invoke an abstract method invokespecial.AbstractParent.foo() : void"
    )
  }

  @Test
  fun abstractMethodInvocationZeroMaximallySpecificMethods() {
    val caller = pluginMethod(pluginClass("mock/plugin/invokespecial/Child", null, PUBLIC_ABSTRACT_CLASS_AF), "zeroMaximallySpecificMethods", "()V", emptyList(), null, PUBLIC_METHOD_AF)

    val problem = AbstractMethodInvocationProblem(
        Location.fromMethod(
            Location.fromClass("invokespecial/SuperInterface", null, IDEA_CLASS_PATH, PUBLIC_INTERFACE_AF),
            "deletedBody",
            "()V",
            emptyList(),
            null,
            PUBLIC_ABSTRACT_METHOD_AF
        ),
        caller,
        Instruction.INVOKE_SPECIAL
    )
    assertProblemFound(problem,
        "Method mock.plugin.invokespecial.Child.zeroMaximallySpecificMethods() : void contains an *invokespecial* instruction referencing a method invokespecial.SuperInterface.deletedBody() : void which doesn't have a non-abstract implementation. This can lead to **AbstractMethodError** exception at runtime.",
        "Attempt to invoke an abstract method invokespecial.SuperInterface.deletedBody() : void"
    )
  }

  @Test
  fun invokeInterfaceOnPrivateMethod() {
    val problem = InvokeInterfaceOnPrivateMethodProblem(
        Location.fromMethod(
            Location.fromClass("statics/MethodBecameStatic", null, IDEA_CLASS_PATH, PUBLIC_INTERFACE_AF),
            "privateInterfaceMethodTestName",
            "()V",
            emptyList(),
            null,
            PUBLIC_ABSTRACT_METHOD_AF
        ),
        pluginMethod(pluginClass("mock/plugin/invokeClassMethodOnInterface/Caller", null, PUBLIC_CLASS_AF), "call4", "(Lstatics/MethodBecameStatic;)V", listOf("b"), null, PUBLIC_METHOD_AF)
    )
    assertProblemFound(problem,
        "Method mock.plugin.invokeClassMethodOnInterface.Caller.call4(MethodBecameStatic b) : void contains an *invokeinterface* instruction referencing a private method statics.MethodBecameStatic.privateInterfaceMethodTestName() : void. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Attempt to execute an *invokeinterface* instruction on a private method statics.MethodBecameStatic.privateInterfaceMethodTestName() : void"
    )
  }

  @Test
  fun missingDefaultConstructor() {
    val caller = pluginMethod(pluginClass("mock/plugin/constructors/MissingDefaultConstructor", null, PUBLIC_CLASS_AF), "<init>", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    val problem = MethodNotFoundProblem(
        SymbolicReference.methodOf("constructors/DeletedDefaultConstructor", "<init>", "()V"),
        caller,
        Instruction.INVOKE_SPECIAL
    )
    assertProblemFound(problem,
        "Method mock.plugin.constructors.MissingDefaultConstructor.<init>() : void contains an *invokespecial* instruction referencing an unresolved method constructors.DeletedDefaultConstructor.<init>() : void. This can lead to **NoSuchMethodError** exception at runtime.",
        "Invocation of unresolved method constructors.DeletedDefaultConstructor.<init>() : void"
    )
  }

  @Test
  fun missingStaticMethod() {
    val caller = pluginMethod(pluginClass("mock/plugin/MethodProblems", null, PUBLIC_CLASS_AF), "brokenNonFoundMethod", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    val problem = MethodNotFoundProblem(
        SymbolicReference.methodOf("com/intellij/openapi/actionSystem/AnAction", "nonExistingMethod", "()V"),
        caller,
        Instruction.INVOKE_STATIC
    )
    assertProblemFound(problem,
        "Method mock.plugin.MethodProblems.brokenNonFoundMethod() : void contains an *invokestatic* instruction referencing an unresolved method com.intellij.openapi.actionSystem.AnAction.nonExistingMethod() : void. This can lead to **NoSuchMethodError** exception at runtime.",
        "Invocation of unresolved method com.intellij.openapi.actionSystem.AnAction.nonExistingMethod() : void"
    )
  }

  @Test
  fun missingVirtualMethod() {
    val caller = pluginMethod(pluginClass("mock/plugin/non/existing/InvokeRemovedMethod", null, PUBLIC_CLASS_AF), "foo", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    val problem = MethodNotFoundProblem(
        SymbolicReference.methodOf("non/existing/Child", "removedMethod", "()V"),
        caller,
        Instruction.INVOKE_VIRTUAL
    )
    assertProblemFound(problem,
        "Method mock.plugin.non.existing.InvokeRemovedMethod.foo() : void contains an *invokevirtual* instruction referencing an unresolved method non.existing.Child.removedMethod() : void. This can lead to **NoSuchMethodError** exception at runtime.",
        "Invocation of unresolved method non.existing.Child.removedMethod() : void"
    )
  }

  @Test
  fun constructorBecamePrivate() {
    val caller = pluginMethod(
        pluginClass("mock/plugin/AccessChangedProblem", null, PUBLIC_CLASS_AF),
        "foo",
        "()V",
        emptyList(),
        null,
        PUBLIC_METHOD_AF
    )
    val problem = IllegalMethodAccessProblem(
        Location.fromMethod(Location.fromClass(
            "com/intellij/openapi/diagnostic/LogUtil",
            null,
            IDEA_CLASS_PATH,
            PUBLIC_CLASS_AF
        ),
            "<init>",
            "()V",
            emptyList(),
            null,
            AccessFlags(0x2)
        ),
        caller,
        Instruction.INVOKE_SPECIAL,
        AccessType.PRIVATE
    )
    assertProblemFound(problem,
        "Method mock.plugin.AccessChangedProblem.foo() : void contains an *invokespecial* instruction referencing a private method com.intellij.openapi.diagnostic.LogUtil.<init>() : void that a class mock.plugin.AccessChangedProblem doesn't have access to. This can lead to **IllegalAccessError** exception at runtime.",
        "Illegal invocation of private method com.intellij.openapi.diagnostic.LogUtil.<init>() : void"
    )
  }

  @Test
  fun illegalAccessToPrivateOrProtectedOrPackagePrivateField() {
    fun accessProblem(methodWithProblem: String, fieldName: String, fieldContainer: String, accessType: AccessType, fieldAccessFlag: AccessFlags): IllegalFieldAccessProblem {
      val accessor = pluginMethod(pluginClass("mock/plugin/field/FieldProblemsContainer", null, PUBLIC_CLASS_AF), methodWithProblem, "()V", emptyList(), null, PUBLIC_METHOD_AF)

      return IllegalFieldAccessProblem(
          Location.fromField(
              Location.fromClass(
                  fieldContainer,
                  null,
                  IDEA_CLASS_PATH,
                  PUBLIC_CLASS_AF
              ),
              fieldName,
              "I",
              null,
              fieldAccessFlag
          ),
          accessor,
          Instruction.GET_FIELD,
          accessType
      )
    }


    assertProblemFound(accessProblem("accessPrivateField", "privateField", "fields/FieldsContainer", AccessType.PRIVATE, AccessFlags(0x2)),
        "Method mock.plugin.field.FieldProblemsContainer.accessPrivateField() : void contains a *getfield* instruction referencing a private field fields.FieldsContainer.privateField : int that a class mock.plugin.field.FieldProblemsContainer doesn't have access to. This can lead to **IllegalAccessError** exception at runtime.",
        "Illegal access of a private field fields.FieldsContainer.privateField : int"
    )

    assertProblemFound(accessProblem("accessProtectedField", "protectedField", "fields/otherPackage/OtherFieldsContainer", AccessType.PROTECTED, AccessFlags(0x4)),
        "Method mock.plugin.field.FieldProblemsContainer.accessProtectedField() : void contains a *getfield* instruction referencing a protected field fields.otherPackage.OtherFieldsContainer.protectedField : int that a class mock.plugin.field.FieldProblemsContainer doesn't have access to. This can lead to **IllegalAccessError** exception at runtime.",
        "Illegal access of a protected field fields.otherPackage.OtherFieldsContainer.protectedField : int"
    )

    assertProblemFound(accessProblem("accessPackageField", "packageField", "fields/otherPackage/OtherFieldsContainer", AccessType.PACKAGE_PRIVATE, AccessFlags(0x0)),
        "Method mock.plugin.field.FieldProblemsContainer.accessPackageField() : void contains a *getfield* instruction referencing a package-private field fields.otherPackage.OtherFieldsContainer.packageField : int that a class mock.plugin.field.FieldProblemsContainer doesn't have access to. This can lead to **IllegalAccessError** exception at runtime.",
        "Illegal access of a package-private field fields.otherPackage.OtherFieldsContainer.packageField : int"
    )
  }

  @Test
  fun nonExistingClassOrInterface() {
    val nonExistingClassRef = ClassReference("non/existing/NonExistingClass")
    val nonExistingException = ClassReference("non/existing/NonExistingException")
    val nonExistingInterface = ClassReference("non/existing/NonExistingInterface")

    val nonExistingClassLocations = listOf(
        pluginMethod(pluginClass("mock/plugin/MethodProblems", null, PUBLIC_CLASS_AF), "brokenReturn", "()Lnon/existing/NonExistingClass;", emptyList(), null, PUBLIC_METHOD_AF),
        pluginMethod(pluginClass("mock/plugin/MethodProblems", null, PUBLIC_CLASS_AF), "brokenArg", "(Lnon/existing/NonExistingClass;)V", listOf("brokenArg"), null, PUBLIC_METHOD_AF),
        pluginMethod(pluginClass("mock/plugin/MethodProblems", null, PUBLIC_CLASS_AF), "brokenLocalVar", "()V", emptyList(), null, PUBLIC_METHOD_AF),
        pluginMethod(pluginClass("mock/plugin/MethodProblems", null, PUBLIC_CLASS_AF), "brokenDotClass", "()V", emptyList(), null, PUBLIC_METHOD_AF),
        pluginMethod(pluginClass("mock/plugin/MethodProblems", null, PUBLIC_CLASS_AF), "brokenMultiArray", "()V", emptyList(), null, PUBLIC_METHOD_AF),
        pluginMethod(pluginClass("mock/plugin/MethodProblems", null, PUBLIC_CLASS_AF), "brokenInvocation", "()V", emptyList(), null, PUBLIC_METHOD_AF),

        pluginMethod(pluginClass("mock/plugin/ParentDoesntExist", null, PUBLIC_CLASS_AF), "<init>", "()V", emptyList(), null, PUBLIC_METHOD_AF),

        pluginMethod(pluginClass("mock/plugin/arrays/ANewArrayInsn", null, PUBLIC_CLASS_AF), "foo", "(JDLjava/lang/Object;)V", listOf("l", "d", "a"), null, PUBLIC_METHOD_AF),

        pluginMethod(pluginClass("mock/plugin/field/FieldProblemsContainer", null, PUBLIC_CLASS_AF), "accessUnknownClassOfArray", "()V", emptyList(), null, PUBLIC_METHOD_AF),
        pluginMethod(pluginClass("mock/plugin/field/FieldProblemsContainer", null, PUBLIC_CLASS_AF), "accessUnknownClass", "()V", emptyList(), null, PUBLIC_METHOD_AF),

        pluginField(pluginClass("mock/plugin/FieldTypeNotFound", null, PUBLIC_CLASS_AF), "myNonExistingClass", "Lnon/existing/NonExistingClass;", null, AccessFlags(0x2)),

        pluginClass("mock/plugin/ParentDoesntExist", null, PUBLIC_CLASS_AF)
    )

    val nonExistingExceptionLocations = listOf(
        pluginMethod(pluginClass("mock/plugin/MethodProblems", null, PUBLIC_CLASS_AF), "brokenThrows", "()V", emptyList(), null, PUBLIC_METHOD_AF),
        pluginMethod(pluginClass("mock/plugin/MethodProblems", null, PUBLIC_CLASS_AF), "brokenCatch", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    )

    val nonExistingInterfaceLocations = listOf(
        pluginClass("mock/plugin/NotFoundInterface", null, PUBLIC_INTERFACE_AF)
    )

    fun getLocationType(it: Location): String = when (it) {
      is ClassLocation -> "Class"
      is MethodLocation -> "Method"
      is FieldLocation -> "Field"
      else -> throw RuntimeException()
    }

    fun getMissingFullDescription(className: String, location: Location) = "${getLocationType(location)} $location references an unresolved class $className. This can lead to **NoSuchClassError** exception at runtime."

    fun getMissingShortDescription(className: String) = "Access to unresolved class $className"

    nonExistingClassLocations.forEach {
      assertProblemFound(ClassNotFoundProblem(nonExistingClassRef, it), getMissingFullDescription("non.existing.NonExistingClass", it), getMissingShortDescription("non.existing.NonExistingClass"))
    }

    nonExistingExceptionLocations.forEach {
      assertProblemFound(ClassNotFoundProblem(nonExistingException, it), getMissingFullDescription("non.existing.NonExistingException", it), getMissingShortDescription("non.existing.NonExistingException"))
    }

    nonExistingInterfaceLocations.forEach {
      assertProblemFound(ClassNotFoundProblem(nonExistingInterface, it), getMissingFullDescription("non.existing.NonExistingInterface", it), getMissingShortDescription("non.existing.NonExistingInterface"))
    }
  }

  @Test
  fun multipleDefaultMethodsOfInvokeSpecial() {
    val caller = pluginMethod(
        pluginClass("mock/plugin/inheritance/SubclassMultipleMethods", null, PUBLIC_CLASS_AF),
        "baz",
        "()V",
        emptyList(),
        null,
        PUBLIC_METHOD_AF
    )
    val problem = MultipleDefaultImplementationsProblem(
        caller,
        SymbolicReference.methodOf("mock/plugin/inheritance/MultipleMethods", "foo", "()V"),
        Instruction.INVOKE_SPECIAL,
        Location.fromMethod(Location.fromClass(
            "inheritance/MultipleDefaultMethod1",
            null,
            IDEA_CLASS_PATH,
            PUBLIC_INTERFACE_AF
        ),
            "foo",
            "()V",
            emptyList(),
            null,
            PUBLIC_METHOD_AF
        ),
        Location.fromMethod(Location.fromClass(
            "inheritance/MultipleDefaultMethod2",
            null,
            IDEA_CLASS_PATH,
            PUBLIC_INTERFACE_AF
        ),
            "foo",
            "()V",
            emptyList(),
            null,
            PUBLIC_METHOD_AF
        )
    )
    assertProblemFound(problem,
        "Method mock.plugin.inheritance.SubclassMultipleMethods.baz() : void contains an *invokespecial* instruction referencing a method reference mock.plugin.inheritance.MultipleMethods.foo() : void which has multiple default implementations: inheritance.MultipleDefaultMethod1.foo() : void and inheritance.MultipleDefaultMethod2.foo() : void. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Multiple default implementations of method mock.plugin.inheritance.MultipleMethods.foo() : void"
    )
  }

  @Test
  fun fieldNotFound() {
    val accessor = pluginMethod(pluginClass("mock/plugin/field/FieldProblemsContainer", null, PUBLIC_CLASS_AF), "accessDeletedField", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    val problem = FieldNotFoundProblem(
        SymbolicReference.fieldOf("fields/FieldsContainer", "deletedField", "I"),
        accessor,
        Instruction.GET_FIELD
    )
    assertProblemFound(problem, "Method $accessor contains a *getfield* instruction referencing an unresolved field fields.FieldsContainer.deletedField : int. This can lead to **NoSuchFieldError** exception at runtime.",
        "Access to unresolved field fields.FieldsContainer.deletedField : int"
    )
  }

  @Test
  fun illegalAccessToPackagePrivateClass() {
    val problem = IllegalClassAccessProblem(
        Location.fromClass("access/other/BecamePackagePrivate", null, IDEA_CLASS_PATH, AccessFlags(0x20)),
        AccessType.PACKAGE_PRIVATE,
        pluginMethod(
            pluginClass("mock/plugin/access/IllegalAccess", null, PUBLIC_CLASS_AF),
            "classBecamePackagePrivate",
            "()V",
            emptyList(),
            null,
            PUBLIC_METHOD_AF
        )
    )
    assertProblemFound(problem,
        "Package-private class access.other.BecamePackagePrivate is not available at mock.plugin.access.IllegalAccess.classBecamePackagePrivate() : void",
        "Illegal access to package-private class access.other.BecamePackagePrivate"
    )
  }
}
