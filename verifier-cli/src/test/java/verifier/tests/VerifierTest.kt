package verifier.tests

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import com.intellij.structure.impl.domain.PluginDependencyImpl
import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.location.*
import com.jetbrains.pluginverifier.problems.*
import com.jetbrains.pluginverifier.reference.ClassReference
import com.jetbrains.pluginverifier.reference.SymbolicReference
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class VerifierTest {

  companion object {
    lateinit var result: VResult.Problems

    lateinit var actualProblems: List<Problem>

    lateinit var redundantProblems: MutableList<Problem>

    @BeforeClass
    @JvmStatic
    fun verifyMockPlugin() {
      prepareTestEnvironment()
      //todo: get rid of this.
      var ideaFile = File("build/mocks/after-idea")
      if (!ideaFile.exists()) {
        ideaFile = File("/home/sergey/Documents/work/intellij-plugin-verifier/verifier-cli/build/mocks/after-idea")
      }
      var pluginFile = File("build/mocks/mock-plugin-1.0.jar")
      if (!pluginFile.exists()) {
        pluginFile = File("/home/sergey/Documents/work/intellij-plugin-verifier/verifier-cli/build/mocks/mock-plugin-1.0.jar")
      }
      val verificationResults = TestResultBuilder.buildResult(ideaFile, pluginFile)
      assertTrue(verificationResults.results.size == 1)
      assertTrue(verificationResults.results[0] is VResult.Problems)
      result = verificationResults.results[0] as VResult.Problems
      actualProblems = result.problems
      redundantProblems = actualProblems.toMutableList()
    }

    private fun prepareTestEnvironment() {
      System.setProperty("plugin.verifier.test.mode", "true")
      System.setProperty("plugin.verifier.test.private.interface.method.name", "privateInterfaceMethodTestName")
    }

    /*
        todo: enable again.
        @AfterClass
        @JvmStatic
        fun assertNoRedundantProblems() {
          val builder = StringBuilder()
          for ((problem, location) in redundantProblems.entries()) {
            builder.append(problem.getDescription()).append("\n").append(" at ").append(location).append("\n")
          }
          assertTrue("Redundant problems: \n$builder", redundantProblems.isEmpty)
        }
    */
    val PUBLIC_CLASS_AF = AccessFlags(0x21)
    val PUBLIC_METHOD_AF = AccessFlags(0x1)
    val PUBLIC_INTERFACE_AF = AccessFlags(0x601)

    val pluginClassPath = ClassPath(ClassPath.Type.ROOT, "mock-plugin-1.0.jar")
    val afterIdeaClassPath = ClassPath(ClassPath.Type.JAR_FILE, "after-idea-1.0.jar")

    fun pluginMethod(
        hostClass: ClassLocation,
        methodName: String,
        methodDescriptor: String,
        parameterNames: List<String>,
        signature: String?,
        accessFlags: AccessFlags
    ): MethodLocation = ProblemLocation.fromMethod(hostClass, methodName, methodDescriptor, parameterNames, signature, accessFlags)

    fun pluginClass(className: String, signature: String?, accessFlags: AccessFlags): ClassLocation = ProblemLocation.fromClass(className, signature, pluginClassPath, accessFlags)

    fun pluginField(
        hostClass: ClassLocation,
        fieldName: String,
        fieldDescriptor: String,
        signature: String?,
        accessFlags: AccessFlags
    ): FieldLocation = ProblemLocation.fromField(hostClass, fieldName, fieldDescriptor, signature, accessFlags)


    fun notImplementedProblems(): Multimap<Problem, ProblemLocation> {
      return builder()
          .put(MethodNotImplementedProblem(
              pluginMethod(
                  ProblemLocation.fromClass("com/intellij/openapi/components/PersistentStateComponent", "<T:Ljava/lang/Object;>Ljava/lang/Object;", afterIdeaClassPath, PUBLIC_INTERFACE_AF),
                  "getState",
                  "()Ljava/lang/Object;",
                  emptyList(),
                  "()TT;",
                  AccessFlags(0x401)
              ), pluginClass("mock/plugin/NotImplementedProblem", null, PUBLIC_CLASS_AF)
          ),
              pluginClass("mock/plugin/NotImplementedProblem", null, PUBLIC_CLASS_AF)
          )
          .put(MethodNotImplementedProblem(
              pluginMethod(
                  ProblemLocation.fromClass("com/intellij/openapi/components/PersistentStateComponent", "<T:Ljava/lang/Object;>Ljava/lang/Object;", afterIdeaClassPath, PUBLIC_INTERFACE_AF),
                  "getState",
                  "()Ljava/lang/Object;",
                  emptyList(),
                  "()TT;",
                  AccessFlags(0x401)
              ), pluginClass("mock/plugin/private_and_static/PrivateAndStaticNotImplemented", null, PUBLIC_CLASS_AF)
          ),
              pluginClass("mock/plugin/private_and_static/PrivateAndStaticNotImplemented", null, PUBLIC_CLASS_AF)
          )
          .build()
    }
//      .put(MethodNotImplementedProblem("com/intellij/openapi/components/PersistentStateComponent", "loadState", "(Ljava/lang/Object;)V"), fromClass("mock/plugin/NotImplementedProblem", null, classPath, PUBLIC_CLASS_AF))
//      .put(MethodNotImplementedProblem("com/intellij/psi/search/UseScopeEnlarger", "getAdditionalUseScope", "(Lcom/intellij/psi/PsiElement;)Lcom/intellij/psi/search/SearchScope;"), fromClass("mock/plugin/abstrackt/NotImplementedAbstractMethod", null, classPath, PUBLIC_CLASS_AF))
//      .put(MethodNotImplementedProblem("com/intellij/openapi/components/PersistentStateComponent", "loadState", "(Ljava/lang/Object;)V"), fromClass("mock/plugin/private_and_static/PrivateAndStaticNotImplemented", "Ljava/lang/Object;Lcom/intellij/openapi/components/PersistentStateComponent<Ljava/lang/String;>;", classPath, PUBLIC_CLASS_AF))
//      .build()


    fun getExpectedProblems(): Multimap<Problem, ProblemLocation> = builder()
//        .putAll(getNoClassProblems())
        .putAll(notImplementedProblems())


        .put(FieldNotFoundProblem("fields/FieldsContainer", "deletedField", "I"), pluginMethod(pluginClass("mock/plugin/field/FieldProblemsContainer", null, PUBLIC_CLASS_AF), "accessDeletedField", "()V", emptyList(), null, PUBLIC_METHOD_AF))//pluginField problems

//      protected members access check


        //missing default constructor
        .build()


    private fun builder(): ImmutableMultimap.Builder<Problem, ProblemLocation> = ImmutableMultimap.builder<Problem, ProblemLocation>()


  }

  @Test
  fun checkMissingDeps() {
    assertFalse(result.dependenciesGraph.start.missingDependencies.isEmpty())
    assertTrue(result.dependenciesGraph.start.missingDependencies.containsKey(PluginDependencyImpl("MissingPlugin", true)))
  }

  private fun assertProblemFound(problem: Problem, expectedEffect: String) {
    assertTrue("${problem.getDescription()} is not found", actualProblems.contains(problem))
    redundantProblems.remove(problem)
    assertEquals(expectedEffect, problem.effect())
  }

  @Test
  fun notImplementedAbstractMethodFromInterface() {
    val notImplementedMethod = ProblemLocation.fromMethod(
        ProblemLocation.fromClass("com/intellij/openapi/components/PersistentStateComponent", "<T:Ljava/lang/Object;>Ljava/lang/Object;", afterIdeaClassPath, PUBLIC_INTERFACE_AF),
        "getState",
        "()Ljava/lang/Object;",
        emptyList(),
        "()TT;",
        AccessFlags(0x401)
    )
    val incompleteClass = pluginClass("mock/plugin/NotImplementedProblem", null, PUBLIC_CLASS_AF)
    assertProblemFound(MethodNotImplementedProblem(notImplementedMethod, incompleteClass),
        "Non-abstract class mock.plugin.NotImplementedProblem inherits from com.intellij.openapi.components.PersistentStateComponent<T> but doesn't implement the abstract method getState() : T. This can lead to **AbstractMethodError** exception at runtime."
    )
  }

  @Test
  fun overridingFinalMethod() {
    val finalMethod = ProblemLocation.fromMethod(
        ProblemLocation.fromClass("com/intellij/openapi/actionSystem/AnAction", null, afterIdeaClassPath, AccessFlags(0x421)),
        "isEnabledInModalContext",
        "()Z",
        emptyList(),
        null,
        AccessFlags(0x11)
    )
    val overridingClass = pluginClass("mock/plugin/OverrideFinalMethodProblem", null, PUBLIC_CLASS_AF)
    val problem = OverridingFinalMethodProblem(finalMethod, overridingClass)
    assertProblemFound(problem, "Class mock.plugin.OverrideFinalMethodProblem overrides the final method com.intellij.openapi.actionSystem.AnAction.isEnabledInModalContext() : boolean. This can lead to **VerifyError** exception at runtime.")
  }

  @Test
  fun staticAccessOfNonStaticField() {
    val accessor = pluginMethod(pluginClass("mock/plugin/field/FieldProblemsContainer", null, PUBLIC_CLASS_AF), "staticAccessOnInstance", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    val problem = StaticAccessOfNonStaticFieldProblem(
        ProblemLocation.fromField(
            ProblemLocation.fromClass("fields/FieldsContainer", null, afterIdeaClassPath, PUBLIC_CLASS_AF),
            "instanceField",
            "I",
            null,
            AccessFlags(0x1)
        ), accessor, Instruction.GET_STATIC
    )
    assertProblemFound(problem, "Method mock.plugin.field.FieldProblemsContainer.staticAccessOnInstance() : void has static access instruction *getstatic* referencing a non-static field fields.FieldsContainer.instanceField : int. This can lead to **IncompatibleClassChangeError** exception at runtime.")
  }

  @Test
  fun changeFinalNonStaticField() {
    val accessor = pluginMethod(pluginClass("mock/plugin/field/FieldProblemsContainer", null, PUBLIC_CLASS_AF), "setOnFinalFieldFromNotInitMethod", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    val problem = ChangeFinalFieldProblem(
        ProblemLocation.fromField(
            ProblemLocation.fromClass("fields/FieldsContainer", null, afterIdeaClassPath, PUBLIC_CLASS_AF),
            "finalField",
            "I",
            null,
            AccessFlags(0x11)
        ), accessor, Instruction.PUT_FIELD
    )
    assertProblemFound(problem, "Method mock.plugin.field.FieldProblemsContainer.setOnFinalFieldFromNotInitMethod() : void has modifying instruction *putfield* referencing a final field fields.FieldsContainer.finalField : int. This can lead to **IllegalAccessError** exception at runtime.")
  }

  @Test
  fun changeFinalStaticField() {
    val accessor = pluginMethod(pluginClass("mock/plugin/field/FieldProblemsContainer", null, PUBLIC_CLASS_AF), "setOnStaticFinalFieldFromNotClinitMethod", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    val problem = ChangeFinalFieldProblem(
        ProblemLocation.fromField(
            ProblemLocation.fromClass("fields/FieldsContainer", null, afterIdeaClassPath, PUBLIC_CLASS_AF),
            "staticFinalField",
            "I",
            null,
            AccessFlags(0x19)
        ),
        accessor,
        Instruction.PUT_STATIC
    )
    assertProblemFound(problem, "Method mock.plugin.field.FieldProblemsContainer.setOnStaticFinalFieldFromNotClinitMethod() : void has modifying instruction *putstatic* referencing a final field fields.FieldsContainer.staticFinalField : int. This can lead to **IllegalAccessError** exception at runtime.")
  }

  @Test
  fun abstractClassInstantiation() {
    val creator = pluginMethod(pluginClass("mock/plugin/news/NewProblems", null, PUBLIC_CLASS_AF), "abstractClass", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    val problem = AbstractClassInstantiationProblem(
        ProblemLocation.fromClass("misc/BecomeAbstract", null, afterIdeaClassPath, AccessFlags(0x421)),
        creator
    )
    assertProblemFound(problem, "Method mock.plugin.news.NewProblems.abstractClass() : void has instantiation *new* instruction referencing an abstract class misc.BecomeAbstract. This can lead to **InstantiationError** exception at runtime.")
  }

  @Test
  fun interfaceInstantiation() {
    val creator = pluginMethod(pluginClass("mock/plugin/news/NewProblems", null, PUBLIC_CLASS_AF), "newInterface", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    val problem = InterfaceInstantiationProblem(
        ProblemLocation.fromClass("misc/BecomeInterface", null, afterIdeaClassPath, AccessFlags(0x601)),
        creator
    )
    assertProblemFound(problem, "Method mock.plugin.news.NewProblems.newInterface() : void has instantiation *new* instruction referencing an interface misc.BecomeInterface. This can lead to **InstantiationError** exception at runtime.")

    val initOnInterfaceMethod = InvokeClassMethodOnInterfaceProblem(
        SymbolicReference.methodOf(
            "misc/BecomeInterface",
            "<init>",
            "()V"
        ),
        creator,
        Instruction.INVOKE_SPECIAL
    )
    assertProblemFound(initOnInterfaceMethod, "Method mock.plugin.news.NewProblems.newInterface() : void has invocation *invokespecial* instruction referencing a *class* method misc.BecomeInterface.<init>() : void, but the method's host misc.BecomeInterface is an *interface*. This can lead to **IncompatibleClassChangeError** at runtime.")
  }

  @Test
  fun nonStaticAccessOfStaticField() {
    val accessor = pluginMethod(pluginClass("mock/plugin/field/FieldProblemsContainer", null, PUBLIC_CLASS_AF), "instanceAccessOnStatic", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    val problem = NonStaticAccessOfStaticFieldProblem(
        ProblemLocation.fromField(
            ProblemLocation.fromClass("fields/FieldsContainer", null, afterIdeaClassPath, PUBLIC_CLASS_AF),
            "staticField",
            "I",
            null,
            AccessFlags(0x9)
        ),
        accessor,
        Instruction.GET_FIELD
    )
    assertProblemFound(problem, "Method mock.plugin.field.FieldProblemsContainer.instanceAccessOnStatic() : void has non-static access instruction *getfield* referencing a static field fields.FieldsContainer.staticField : int. This can lead to **IncompatibleClassChangeError** exception at runtime.")
  }

  @Test
  fun superClassBecameInterface() {
    val problem = SuperClassBecameInterfaceProblem(
        pluginClass("mock/plugin/inheritance/SuperClassBecameInterface", null, PUBLIC_CLASS_AF),
        ProblemLocation.fromClass("misc/BecomeInterface", null, afterIdeaClassPath, AccessFlags(0x601))
    )
    assertProblemFound(problem, "Class mock.plugin.inheritance.SuperClassBecameInterface has a *super class* misc.BecomeInterface which is actually an *interface*. This can lead to **IncompatibleClassChangeError** at runtime.")
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
    assertProblemFound(problem, "Method mock.plugin.invokeClassMethodOnInterface.Caller.call(BecomeInterface b) : void has invocation *invokevirtual* instruction referencing a *class* method misc.BecomeInterface.invokeVirtualMethod() : void, but the method's host misc.BecomeInterface is an *interface*. This can lead to **IncompatibleClassChangeError** at runtime.")
  }

  @Test
  fun superInterfaceBecameClass() {
    val problem = SuperInterfaceBecameClassProblem(
        pluginClass("mock/plugin/inheritance/SuperInterfaceBecomeClass", null, AccessFlags(0x601)),
        ProblemLocation.fromClass("misc/BecomeClass", null, afterIdeaClassPath, PUBLIC_CLASS_AF)
    )
    assertProblemFound(problem, "Interface mock.plugin.inheritance.SuperInterfaceBecomeClass has a *super interface* misc.BecomeClass which is actually a *class*. This can lead to **IncompatibleClassChangeError** exception at runtime.")
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
    assertProblemFound(problem, "Method mock.plugin.invokeClassMethodOnInterface.Caller.call2(BecomeClass b) : void has invocation *invokeinterface* instruction referencing an *interface* method misc.BecomeClass.invokeInterfaceOnClass() : void, but the method's host misc.BecomeClass is a *class*. This can lead to **IncompatibleClassChangeError** at runtime.")
  }


  @Test
  fun inheritsFromFinalClass() {
    val pluginClass = pluginClass("mock/plugin/finals/InheritFromFinalClass", null, PUBLIC_CLASS_AF)
    val problem = InheritFromFinalClassProblem(
        pluginClass,
        ProblemLocation.fromClass("finals/BecomeFinal", null, afterIdeaClassPath, AccessFlags(0x31))
    )
    assertProblemFound(problem, "Class mock.plugin.finals.InheritFromFinalClass inherits from a final class finals.BecomeFinal. This can lead to **VerifyError** exception at runtime.")
  }

  @Test
  fun invokeStaticOnNonStaticMethod() {
    val problem = InvokeStaticOnNonStaticMethodProblem(
        ProblemLocation.fromMethod(
            ProblemLocation.fromClass("invocation/InvocationProblems", null, afterIdeaClassPath, PUBLIC_CLASS_AF),
            "wasStatic",
            "()V",
            emptyList(),
            null,
            PUBLIC_METHOD_AF
        ),
        pluginMethod(pluginClass("mock/plugin/invokeStaticOnInstance/InvocationProblemsUser", null, PUBLIC_CLASS_AF), "foo", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    )
    assertProblemFound(problem, "Method mock.plugin.invokeStaticOnInstance.InvocationProblemsUser.foo() : void contains an *invokestatic* instruction referencing a non-static method invocation.InvocationProblems.wasStatic() : void. This can lead to **IncompatibleClassChangeError** exception at runtime.")
  }

  @Test
  fun invokeVirtualOnStaticMethod() {
    val problem = InvokeNonStaticInstructionOnStaticMethodProblem(
        ProblemLocation.fromMethod(
            ProblemLocation.fromClass("com/intellij/lang/SmartEnterProcessor", null, afterIdeaClassPath, AccessFlags(0x421)),
            "commit",
            "()V",
            emptyList(),
            null,
            AccessFlags(0x9)
        ),
        pluginMethod(pluginClass("mock/plugin/invokeVirtualOnStatic/SmartEnterProcessorUser", null, PUBLIC_CLASS_AF), "main", "()V", emptyList(), null, PUBLIC_METHOD_AF),
        Instruction.INVOKE_VIRTUAL
    )
    assertProblemFound(problem, "Method mock.plugin.invokeVirtualOnStatic.SmartEnterProcessorUser.main() : void contains an *invokevirtual* instruction referencing a static method com.intellij.lang.SmartEnterProcessor.commit() : void. This can lead to **IncompatibleClassChangeError** exception at runtime.")
  }

  @Test
  fun invokeSpecialOnStaticMethod() {
    val problem = InvokeNonStaticInstructionOnStaticMethodProblem(
        ProblemLocation.fromMethod(
            ProblemLocation.fromClass("invokespecial/AbstractParent", null, afterIdeaClassPath, AccessFlags(0x421)),
            "becomeStatic",
            "()V",
            emptyList(),
            null,
            AccessFlags(0x9)
        ),
        pluginMethod(pluginClass("mock/plugin/invokespecial/Child", null, AccessFlags(0x421)), "invokeSpecialOnStaticMethod", "()V", emptyList(), null, PUBLIC_METHOD_AF),
        Instruction.INVOKE_SPECIAL
    )
    assertProblemFound(problem, "Method mock.plugin.invokespecial.Child.invokeSpecialOnStaticMethod() : void contains an *invokespecial* instruction referencing a static method invokespecial.AbstractParent.becomeStatic() : void. This can lead to **IncompatibleClassChangeError** exception at runtime.")
  }

  @Test
  fun invokeInterfaceOnStaticMethod() {
    val problem = InvokeNonStaticInstructionOnStaticMethodProblem(
        ProblemLocation.fromMethod(
            ProblemLocation.fromClass("statics/MethodBecameStatic", null, afterIdeaClassPath, AccessFlags(0x601)),
            "becomeStatic",
            "()V",
            emptyList(),
            null,
            AccessFlags(0x9)
        ),
        pluginMethod(pluginClass("mock/plugin/invokeClassMethodOnInterface/Caller", null, PUBLIC_CLASS_AF), "call3", "(Lstatics/MethodBecameStatic;)V", listOf("b"), null, PUBLIC_METHOD_AF),
        Instruction.INVOKE_INTERFACE
    )
    assertProblemFound(problem, "Method mock.plugin.invokeClassMethodOnInterface.Caller.call3(MethodBecameStatic b) : void contains an *invokeinterface* instruction referencing a static method statics.MethodBecameStatic.becomeStatic() : void. This can lead to **IncompatibleClassChangeError** exception at runtime.")
  }

  @Test
  fun abstractMethodInvocation() {
    val caller = pluginMethod(pluginClass("mock/plugin/invokespecial/Child", null, AccessFlags(0x421)), "bar", "()V", emptyList(), null, PUBLIC_METHOD_AF)

    val problem = AbstractMethodInvocationProblem(
        ProblemLocation.fromMethod(
            ProblemLocation.fromClass("invokespecial/AbstractParent", null, afterIdeaClassPath, AccessFlags(0x421)),
            "foo",
            "()V",
            emptyList(),
            null,
            AccessFlags(0x401)
        ),
        caller,
        Instruction.INVOKE_SPECIAL
    )
    assertProblemFound(problem, "Method mock.plugin.invokespecial.Child.bar() : void contains an *invokespecial* instruction referencing a method invokespecial.AbstractParent.foo() : void which doesn't have a non-abstract implementation. This can lead to **AbstractMethodError** exception at runtime.")
  }

  @Test
  fun abstractMethodInvocationZeroMaximallySpecificMethods() {
    val caller = pluginMethod(pluginClass("mock/plugin/invokespecial/Child", null, AccessFlags(0x421)), "zeroMaximallySpecificMethods", "()V", emptyList(), null, PUBLIC_METHOD_AF)

    val problem = AbstractMethodInvocationProblem(
        ProblemLocation.fromMethod(
            ProblemLocation.fromClass("invokespecial/SuperInterface", null, afterIdeaClassPath, AccessFlags(0x601)),
            "deletedBody",
            "()V",
            emptyList(),
            null,
            AccessFlags(0x401)
        ),
        caller,
        Instruction.INVOKE_SPECIAL
    )
    assertProblemFound(problem, "Method mock.plugin.invokespecial.Child.zeroMaximallySpecificMethods() : void contains an *invokespecial* instruction referencing a method invokespecial.SuperInterface.deletedBody() : void which doesn't have a non-abstract implementation. This can lead to **AbstractMethodError** exception at runtime.")
  }

  @Test
  fun invokeInterfaceOnPrivateMethod() {
    val problem = InvokeInterfaceOnPrivateMethodProblem(
        ProblemLocation.fromMethod(
            ProblemLocation.fromClass("statics/MethodBecameStatic", null, afterIdeaClassPath, AccessFlags(0x601)),
            "privateInterfaceMethodTestName",
            "()V",
            emptyList(),
            null,
            AccessFlags(0x401)
        ),
        pluginMethod(pluginClass("mock/plugin/invokeClassMethodOnInterface/Caller", null, PUBLIC_CLASS_AF), "call4", "(Lstatics/MethodBecameStatic;)V", listOf("b"), null, PUBLIC_METHOD_AF)
    )
    assertProblemFound(problem, "Method mock.plugin.invokeClassMethodOnInterface.Caller.call4(MethodBecameStatic b) : void contains an *invokeinterface* instruction referencing a private method statics.MethodBecameStatic.privateInterfaceMethodTestName() : void. This can lead to **IncompatibleClassChangeError** exception at runtime.")
  }

  @Test
  fun missingDefaultConstructor() {
    val caller = pluginMethod(pluginClass("mock/plugin/constructors/MissingDefaultConstructor", null, PUBLIC_CLASS_AF), "<init>", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    val problem = MethodNotFoundProblem(
        SymbolicReference.methodOf("constructors/DeletedDefaultConstructor", "<init>", "()V"),
        caller,
        Instruction.INVOKE_SPECIAL
    )
    assertProblemFound(problem, "Method mock.plugin.constructors.MissingDefaultConstructor.<init>() : void contains an *invokespecial* instruction referencing an unresolved method constructors.DeletedDefaultConstructor.<init>() : void. This can lead to **NoSuchMethodError** exception at runtime.")
  }

  @Test
  fun missingStaticMethod() {
    val caller = pluginMethod(pluginClass("mock/plugin/MethodProblems", null, PUBLIC_CLASS_AF), "brokenNonFoundMethod", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    val problem = MethodNotFoundProblem(
        SymbolicReference.methodOf("com/intellij/openapi/actionSystem/AnAction", "nonExistingMethod", "()V"),
        caller,
        Instruction.INVOKE_STATIC
    )
    assertProblemFound(problem, "Method mock.plugin.MethodProblems.brokenNonFoundMethod() : void contains an *invokestatic* instruction referencing an unresolved method com.intellij.openapi.actionSystem.AnAction.nonExistingMethod() : void. This can lead to **NoSuchMethodError** exception at runtime.")
  }

  @Test
  fun missingVirtualMethod() {
    val caller = pluginMethod(pluginClass("mock/plugin/non/existing/InvokeRemovedMethod", null, PUBLIC_CLASS_AF), "foo", "()V", emptyList(), null, PUBLIC_METHOD_AF)
    val problem = MethodNotFoundProblem(
        SymbolicReference.methodOf("non/existing/Child", "removedMethod", "()V"),
        caller,
        Instruction.INVOKE_VIRTUAL
    )
    assertProblemFound(problem, "Method mock.plugin.non.existing.InvokeRemovedMethod.foo() : void contains an *invokevirtual* instruction referencing an unresolved method non.existing.Child.removedMethod() : void. This can lead to **NoSuchMethodError** exception at runtime.")
  }

  //    .put(IllegalFieldAccessProblem("mock/plugin/access/Point3d", "x", "I", AccessType.PROTECTED), ProblemLocation.Companion.fromMethod("mock/plugin/access/Point3d", "delta", "(Laccess/points/Point;)V"))
//      .put(IllegalFieldAccessProblem("access/AccessProblemBase", "x", "I", AccessType.PROTECTED), pluginMethod("mock/plugin/access/IllegalAccess", "main", "([Ljava/lang/String;)V"))
//      .put(IllegalMethodAccessProblem("access/AccessProblemBase", "foo", "()V", AccessType.PROTECTED), pluginMethod("mock/plugin/access/IllegalAccess", "main", "([Ljava/lang/String;)V"))


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
        ProblemLocation.fromMethod(ProblemLocation.fromClass(
            "com/intellij/openapi/diagnostic/LogUtil",
            null,
            afterIdeaClassPath,
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
    assertProblemFound(problem, "Method mock.plugin.AccessChangedProblem.foo() : void contains an *invokespecial* instruction referencing a private method com.intellij.openapi.diagnostic.LogUtil.<init>() : void that a class mock.plugin.AccessChangedProblem doesn't have access to. This can lead to **IllegalAccessError** exception at runtime.")
  }

  @Test
  fun illegalAccessToPrivateOrProtectedOrPackagePrivateField() {

    fun accessProblem(methodWithProblem: String, fieldName: String, fieldContainer: String, accessType: AccessType, fieldAccessFlag: AccessFlags): IllegalFieldAccessProblem {
      val accessor = pluginMethod(pluginClass("mock/plugin/field/FieldProblemsContainer", null, PUBLIC_CLASS_AF), methodWithProblem, "()V", emptyList(), null, PUBLIC_METHOD_AF)

      return IllegalFieldAccessProblem(
          ProblemLocation.fromField(
              ProblemLocation.fromClass(
                  fieldContainer,
                  null,
                  afterIdeaClassPath,
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
        "Method mock.plugin.field.FieldProblemsContainer.accessPrivateField() : void contains a *getfield* instruction referencing a private field fields.FieldsContainer.privateField : int that a class mock.plugin.field.FieldProblemsContainer doesn't have access to. This can lead to **IllegalAccessError** exception at runtime.")

    assertProblemFound(accessProblem("accessProtectedField", "protectedField", "fields/otherPackage/OtherFieldsContainer", AccessType.PROTECTED, AccessFlags(0x4)),
        "Method mock.plugin.field.FieldProblemsContainer.accessProtectedField() : void contains a *getfield* instruction referencing a protected field fields.otherPackage.OtherFieldsContainer.protectedField : int that a class mock.plugin.field.FieldProblemsContainer doesn't have access to. This can lead to **IllegalAccessError** exception at runtime.")

    assertProblemFound(accessProblem("accessPackageField", "packageField", "fields/otherPackage/OtherFieldsContainer", AccessType.PACKAGE_PRIVATE, AccessFlags(0x0)),
        "Method mock.plugin.field.FieldProblemsContainer.accessPackageField() : void contains a *getfield* instruction referencing a package-private field fields.otherPackage.OtherFieldsContainer.packageField : int that a class mock.plugin.field.FieldProblemsContainer doesn't have access to. This can lead to **IllegalAccessError** exception at runtime.")
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
        pluginClass("mock/plugin/NotFoundInterface", null, AccessFlags(0x601))
    )

    fun getLocationType(it: ProblemLocation): String = when (it) {
      is ClassLocation -> "Class"
      is MethodLocation -> "Method"
      is FieldLocation -> "Field"
      else -> throw RuntimeException()
    }

    fun getMissingClassEffect(className: String, location: ProblemLocation) = "${getLocationType(location)} $location references an unresolved class $className. This can lead to **NoSuchClassError** exception at runtime."

    nonExistingClassLocations.forEach {
      assertProblemFound(ClassNotFoundProblem(nonExistingClassRef, it), getMissingClassEffect("non.existing.NonExistingClass", it))
    }

    nonExistingExceptionLocations.forEach {
      assertProblemFound(ClassNotFoundProblem(nonExistingException, it), getMissingClassEffect("non.existing.NonExistingException", it))
    }

    nonExistingInterfaceLocations.forEach {
      assertProblemFound(ClassNotFoundProblem(nonExistingInterface, it), getMissingClassEffect("non.existing.NonExistingInterface", it))
    }
  }


}
