package verifier.tests

import com.intellij.structure.impl.domain.PluginDependencyImpl
import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.location.AccessFlags
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.*
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
        ProblemLocation.fromClass("com/intellij/openapi/components/PersistentStateComponent", "<T:Ljava/lang/Object;>Ljava/lang/Object;", EContainer.afterIdeaClassPath, EContainer.PUBLIC_INTERFACE_AF),
        "getState",
        "()Ljava/lang/Object;",
        emptyList(),
        "()TT;",
        AccessFlags(0x401)
    )
    val incompleteClass = EContainer.pluginClass("mock/plugin/NotImplementedProblem", null, EContainer.PUBLIC_CLASS_AF)
    assertProblemFound(MethodNotImplementedProblem(notImplementedMethod, incompleteClass),
        "Non-abstract class mock.plugin.NotImplementedProblem inherits com.intellij.openapi.components.PersistentStateComponent<T> but doesn't implement the abstract method getState() : T. This can lead to **AbstractMethodError** exception at runtime."
    )
  }

  @Test
  fun overridingFinalMethod() {
    val finalMethod = ProblemLocation.fromMethod(
        ProblemLocation.fromClass("com/intellij/openapi/actionSystem/AnAction", null, EContainer.afterIdeaClassPath, AccessFlags(0x421)),
        "isEnabledInModalContext",
        "()Z",
        emptyList(),
        null,
        AccessFlags(0x11)
    )
    val overridingClass = EContainer.pluginClass("mock/plugin/OverrideFinalMethodProblem", null, EContainer.PUBLIC_CLASS_AF)
    val problem = OverridingFinalMethodProblem(finalMethod, overridingClass)
    assertProblemFound(problem, "Class mock.plugin.OverrideFinalMethodProblem overrides the final method com.intellij.openapi.actionSystem.AnAction.isEnabledInModalContext() : boolean. This can lead to **VerifyError** exception at runtime.")
  }

  @Test
  fun staticAccessOfNonStaticField() {
    val accessor = EContainer.pluginMethod(EContainer.pluginClass("mock/plugin/field/FieldProblemsContainer", null, EContainer.PUBLIC_CLASS_AF), "staticAccessOnInstance", "()V", emptyList(), null, EContainer.PUBLIC_METHOD_AF)
    val problem = StaticAccessOfNonStaticFieldProblem(
        ProblemLocation.fromField(
            ProblemLocation.fromClass("fields/FieldsContainer", null, EContainer.afterIdeaClassPath, EContainer.PUBLIC_CLASS_AF),
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
    val accessor = EContainer.pluginMethod(EContainer.pluginClass("mock/plugin/field/FieldProblemsContainer", null, EContainer.PUBLIC_CLASS_AF), "setOnFinalFieldFromNotInitMethod", "()V", emptyList(), null, EContainer.PUBLIC_METHOD_AF)
    val problem = ChangeFinalFieldProblem(
        ProblemLocation.fromField(
            ProblemLocation.fromClass("fields/FieldsContainer", null, EContainer.afterIdeaClassPath, EContainer.PUBLIC_CLASS_AF),
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
    val accessor = EContainer.pluginMethod(EContainer.pluginClass("mock/plugin/field/FieldProblemsContainer", null, EContainer.PUBLIC_CLASS_AF), "setOnStaticFinalFieldFromNotClinitMethod", "()V", emptyList(), null, EContainer.PUBLIC_METHOD_AF)
    val problem = ChangeFinalFieldProblem(
        ProblemLocation.fromField(
            ProblemLocation.fromClass("fields/FieldsContainer", null, EContainer.afterIdeaClassPath, EContainer.PUBLIC_CLASS_AF),
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
    val creator = EContainer.pluginMethod(EContainer.pluginClass("mock/plugin/news/NewProblems", null, EContainer.PUBLIC_CLASS_AF), "abstractClass", "()V", emptyList(), null, EContainer.PUBLIC_METHOD_AF)
    val problem = AbstractClassInstantiationProblem(
        ProblemLocation.fromClass("misc/BecomeAbstract", null, EContainer.afterIdeaClassPath, AccessFlags(0x421)),
        creator
    )
    assertProblemFound(problem, "Method mock.plugin.news.NewProblems.abstractClass() : void has instantiation *new* instruction referencing an abstract class misc.BecomeAbstract. This can lead to **InstantiationError** exception at runtime.")
  }

  @Test
  fun interfaceInstantiation() {
    val creator = EContainer.pluginMethod(EContainer.pluginClass("mock/plugin/news/NewProblems", null, EContainer.PUBLIC_CLASS_AF), "newInterface", "()V", emptyList(), null, EContainer.PUBLIC_METHOD_AF)
    val problem = InterfaceInstantiationProblem(
        ProblemLocation.fromClass("misc/BecomeInterface", null, EContainer.afterIdeaClassPath, AccessFlags(0x601)),
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
    val accessor = EContainer.pluginMethod(EContainer.pluginClass("mock/plugin/field/FieldProblemsContainer", null, EContainer.PUBLIC_CLASS_AF), "instanceAccessOnStatic", "()V", emptyList(), null, EContainer.PUBLIC_METHOD_AF)
    val problem = NonStaticAccessOfStaticFieldProblem(
        ProblemLocation.fromField(
            ProblemLocation.fromClass("fields/FieldsContainer", null, EContainer.afterIdeaClassPath, EContainer.PUBLIC_CLASS_AF),
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
        EContainer.pluginClass("mock/plugin/inheritance/SuperClassBecameInterface", null, EContainer.PUBLIC_CLASS_AF),
        ProblemLocation.fromClass("misc/BecomeInterface", null, EContainer.afterIdeaClassPath, AccessFlags(0x601))
    )
    assertProblemFound(problem, "Class mock.plugin.inheritance.SuperClassBecameInterface has a *super class* misc.BecomeInterface which is actually an *interface*. This can lead to **IncompatibleClassChangeError** at runtime.")
  }

  @Test
  fun invokeClassMethodOnInterface() {
    val caller = EContainer.pluginMethod(
        EContainer.pluginClass("mock/plugin/invokeClassMethodOnInterface/Caller", null, EContainer.PUBLIC_CLASS_AF),
        "call",
        "(Lmisc/BecomeInterface;)V",
        listOf("b"),
        null,
        EContainer.PUBLIC_METHOD_AF
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
        EContainer.pluginClass("mock/plugin/inheritance/SuperInterfaceBecomeClass", null, AccessFlags(0x601)),
        ProblemLocation.fromClass("misc/BecomeClass", null, EContainer.afterIdeaClassPath, EContainer.PUBLIC_CLASS_AF)
    )
    assertProblemFound(problem, "Interface mock.plugin.inheritance.SuperInterfaceBecomeClass has a *super interface* misc.BecomeClass which is actually a *class*. This can lead to **IncompatibleClassChangeError** exception at runtime.")
  }

  @Test
  fun invokeInterfaceMethodOnClass() {
    val caller = EContainer.pluginMethod(
        EContainer.pluginClass("mock/plugin/invokeClassMethodOnInterface/Caller", null, EContainer.PUBLIC_CLASS_AF),
        "call2",
        "(Lmisc/BecomeClass;)V",
        listOf("b"),
        null,
        EContainer.PUBLIC_METHOD_AF
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



}
