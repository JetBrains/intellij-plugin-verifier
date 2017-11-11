package com.jetbrains.pluginverifier.tests

import com.google.common.io.Files
import com.jetbrains.plugin.structure.classes.resolvers.EmptyResolver
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.core.Verification
import com.jetbrains.pluginverifier.core.VerifierTask
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.parameters.VerifierParameters
import com.jetbrains.pluginverifier.parameters.ide.IdeCreator
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.plugin.PluginDetailsProviderImpl
import com.jetbrains.pluginverifier.reporting.verification.VerificationReportageImpl
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.problems.Problem
import com.jetbrains.pluginverifier.tests.mocks.EmptyReporterSetProvider
import com.jetbrains.pluginverifier.tests.mocks.NotFoundDependencyFinder
import com.jetbrains.pluginverifier.tests.mocks.TestJdkDescriptorProvider
import org.hamcrest.core.Is.`is`
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class VerificationCorrectnessTest {

  companion object {
    var shouldCheckRedundantProblems: Boolean = false

    lateinit var result: Verdict.MissingDependencies

    lateinit var actualProblems: Set<Problem>

    lateinit var actualDeprecatedUsages: Set<DeprecatedApiUsage>

    /**
     * The list is required here to take repetition of the problem into account.
     */
    lateinit var redundantProblems: MutableList<Problem>

    lateinit var redundantDeprecated: MutableList<DeprecatedApiUsage>

    private fun doIdeaAndPluginVerification(ideaFile: File, pluginFile: File): Result {
      val pluginCoordinate = PluginCoordinate.ByFile(pluginFile)
      val jdkDescriptor = TestJdkDescriptorProvider.getJdkDescriptorForTests()

      val tempFolder = Files.createTempDir()
      tempFolder.deleteOnExit()
      val pluginDetailsProvider = PluginDetailsProviderImpl(tempFolder)
      return IdeCreator.createByFile(ideaFile, IdeVersion.createIdeVersion("IU-145.500")).use { ideDescriptor ->
        val externalClassesPrefixes = OptionsParser.getExternalClassesPrefixes(CmdOpts())
        val verifierParams = VerifierParameters(
            externalClassesPrefixes,
            emptyList(),
            EmptyResolver,
            true
        )
        val tasks = listOf(VerifierTask(pluginCoordinate, ideDescriptor, NotFoundDependencyFinder()))

        VerificationReportageImpl(EmptyReporterSetProvider).use { verificationReportage ->
          Verification.run(verifierParams, pluginDetailsProvider, tasks, verificationReportage, jdkDescriptor).single()
        }
      }
    }

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
      val verificationResult = doIdeaAndPluginVerification(ideaFile, pluginFile)
      val verdict = verificationResult.verdict
      assertTrue(verdict is Verdict.MissingDependencies)
      result = verdict as Verdict.MissingDependencies
      actualProblems = result.problems
      actualDeprecatedUsages = result.deprecatedUsages
      redundantProblems = actualProblems.toMutableList()
      redundantDeprecated = actualDeprecatedUsages.toMutableList()
    }

    private fun prepareTestEnvironment() {
      System.setProperty("plugin.verifier.test.mode", "true")
      System.setProperty("plugin.verifier.test.private.interface.method.name", "privateInterfaceMethodTestName")
    }

    @AfterClass
    @JvmStatic
    fun assertNoRedundantProblemsAndDeprecatedUsages() {
      if (shouldCheckRedundantProblems) {
        val message = redundantProblems.joinToString(separator = "\n") { "${it.shortDescription}:\n    ${it.fullDescription}" }
        assertTrue("Redundant problems: \n$message", redundantProblems.isEmpty())

        val deprecatedMessage = redundantDeprecated.joinToString(separator = "\n") { "${it.fullDescription}\n" }
        assertTrue("Redundant deprecated usages found: \n" + deprecatedMessage, redundantDeprecated.isEmpty())
      }
    }
  }

  @Test
  fun checkMissingDeps() {
    val missingDependencies = result.directMissingDependencies
    assertFalse(missingDependencies.isEmpty())
    println(missingDependencies)
    val expectedDep = setOf(MissingDependency(PluginDependencyImpl("MissingPlugin", true, false), "Plugin MissingPlugin doesn't have a build compatible with IU-145.500"))
    assertEquals(expectedDep, missingDependencies.toSet())
  }

  private fun assertProblemFound(expectedFullDescription: String, expectedShortDescription: String) {
    val problem = actualProblems.find { it.shortDescription == expectedShortDescription && it.fullDescription == expectedFullDescription }
    assertNotNull("Problem is not found:\n$expectedFullDescription\nall problems: ${actualProblems.joinToString("\n") { it.fullDescription }}", problem)
    redundantProblems.remove(problem)
    assertThat(problem!!.shortDescription, `is`(expectedShortDescription))
    assertThat(problem.fullDescription, `is`(expectedFullDescription))
  }

  private fun assertDeprecatedUsageFound(description: String) {
    val foundDeprecatedUsage = actualDeprecatedUsages.find { description == it.fullDescription }
    assertTrue("Deprecated is not found:\n$description\nall deprecated:\n" + actualDeprecatedUsages.joinToString("\n"), foundDeprecatedUsage != null)
    redundantDeprecated.remove(foundDeprecatedUsage)
  }

  @Test
  fun `test is run on the whole class`() {
    shouldCheckRedundantProblems = true
  }

  @Test
  fun `deprecated method is used`() {
    //Auxiliary deprecated usage.
    assertDeprecatedUsageFound("Deprecated class deprecated.DeprecatedClass is used in mock.plugin.deprecated.DeprecatedUser.method() : void")
    assertDeprecatedUsageFound("Method <init>() : void of the deprecated class deprecated.DeprecatedClass is used in mock.plugin.deprecated.DeprecatedUser.method() : void")

    assertDeprecatedUsageFound("Deprecated method deprecated.DeprecatedMethod.foo(int x) : void is used in mock.plugin.deprecated.DeprecatedUser.method() : void")
  }

  @Test
  fun `usage of the method of the deprecated class`() {
    assertDeprecatedUsageFound("Method foo() : void of the deprecated class deprecated.DeprecatedClass is used in mock.plugin.deprecated.DeprecatedUser.method() : void")
  }

  @Test
  fun `deprecated constructor is used`() {
    assertDeprecatedUsageFound("Deprecated method deprecated.DeprecatedMethod.<init>() : void is used in mock.plugin.deprecated.DeprecatedUser.method() : void")
  }

  @Test
  fun `overriding deprecated method`() {
    assertDeprecatedUsageFound("Deprecated method deprecated.DeprecatedMethod.foo(int x) : void is overridden in class mock.plugin.deprecated.OverrideDeprecatedMethod")
  }

  @Test
  fun `overriding deprecated interface method`() {
    assertDeprecatedUsageFound("Deprecated class deprecated.DeprecatedInterface is used in mock.plugin.deprecated.OverrideDeprecatedMethod")
    assertDeprecatedUsageFound("Deprecated method deprecated.DeprecatedInterface.bar() : void is overridden in class mock.plugin.deprecated.OverrideDeprecatedMethod")
  }


  @Test
  fun `use default deprecated constructor`() {
    assertDeprecatedUsageFound("Deprecated method deprecated.DeprecatedMethod.<init>() : void is used in mock.plugin.deprecated.OverrideDeprecatedMethod.<init>() : void")
  }

  @Test
  fun `deprecated class is used`() {
    //Auxiliary deprecated usage.
    assertDeprecatedUsageFound("Method <init>() : void of the deprecated class deprecated.DeprecatedClass is used in mock.plugin.deprecated.DeprecatedUser.clazz() : void")

    assertDeprecatedUsageFound("Deprecated class deprecated.DeprecatedClass is used in mock.plugin.deprecated.DeprecatedUser.clazz() : void")
  }

  @Test
  fun `deprecated with comment class is used`() {
    //Auxiliary deprecated usage.
    assertDeprecatedUsageFound("Method <init>() : void of the deprecated class deprecated.DeprecatedWithCommentClass is used in mock.plugin.deprecated.DeprecatedUser.clazzWithComment() : void")

    assertDeprecatedUsageFound("Deprecated class deprecated.DeprecatedWithCommentClass is used in mock.plugin.deprecated.DeprecatedUser.clazzWithComment() : void")
  }


  @Test
  fun `deprecated field usage`() {
    assertDeprecatedUsageFound("Deprecated field deprecated.DeprecatedField.x : int is used in mock.plugin.deprecated.DeprecatedUser.field() : void")
  }

  @Test
  fun `field of the deprecated class usage`() {
    //Auxiliary usages
    assertDeprecatedUsageFound("Deprecated class deprecated.DeprecatedClass is used in mock.plugin.deprecated.DeprecatedUser.field() : void")
    assertDeprecatedUsageFound("Method <init>() : void of the deprecated class deprecated.DeprecatedClass is used in mock.plugin.deprecated.DeprecatedUser.field() : void")

    assertDeprecatedUsageFound("Field x : int of the deprecated class deprecated.DeprecatedClass is used in mock.plugin.deprecated.DeprecatedUser.field() : void")
  }

  @Test
  fun notImplementedAbstractMethodFromInterface() {
    assertProblemFound("Non-abstract class mock.plugin.NotImplementedProblem inherits from com.intellij.openapi.components.PersistentStateComponent<T> but doesn't implement the abstract method getState() : T. This can lead to **AbstractMethodError** exception at runtime.",
        "Abstract method com.intellij.openapi.components.PersistentStateComponent<T>.getState() : T is not implemented"
    )
  }

  @Test
  fun notImplementedPrivateOverridingFromInterface() {
    assertProblemFound("Non-abstract class mock.plugin.private_and_static.PrivateOverridingNotImplemented inherits from com.intellij.openapi.components.PersistentStateComponent<T> but doesn't implement the abstract method getState() : T. This can lead to **AbstractMethodError** exception at runtime.", "Abstract method com.intellij.openapi.components.PersistentStateComponent<T>.getState() : T is not implemented")
  }

  @Test
  fun notImplementedStaticOverridingFromInterface() {
    assertProblemFound("Non-abstract class mock.plugin.private_and_static.StaticOverridingNotImplemented inherits from com.intellij.openapi.components.PersistentStateComponent<T> but doesn't implement the abstract method getState() : T. This can lead to **AbstractMethodError** exception at runtime.", "Abstract method com.intellij.openapi.components.PersistentStateComponent<T>.getState() : T is not implemented")
  }


  @Test
  fun notImplementedAbstractMethodFromAbstractClass() {
    assertProblemFound("Non-abstract class mock.plugin.abstrackt.NotImplementedAbstractMethod inherits from com.intellij.psi.search.UseScopeEnlarger but doesn't implement the abstract method getAdditionalUseScope(PsiElement arg0) : SearchScope. This can lead to **AbstractMethodError** exception at runtime.",
        "Abstract method com.intellij.psi.search.UseScopeEnlarger.getAdditionalUseScope(PsiElement arg0) : SearchScope is not implemented"
    )
  }


  @Test
  fun overridingFinalMethod() {
    assertProblemFound("Class mock.plugin.OverrideFinalMethodProblem overrides the final method com.intellij.openapi.actionSystem.AnAction.isEnabledInModalContext() : boolean. This can lead to **VerifyError** exception at runtime.",
        "Overriding a final method com.intellij.openapi.actionSystem.AnAction.isEnabledInModalContext() : boolean"
    )
  }

  @Test
  fun staticAccessOfNonStaticField() {
    assertProblemFound("Method mock.plugin.field.FieldProblemsContainer.staticAccessOnInstance() : void has static access instruction *getstatic* referencing a non-static field fields.FieldsContainer.instanceField : int. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Attempt to execute a static access instruction *getstatic* on a non-static field fields.FieldsContainer.instanceField : int"
    )
  }

  @Test
  fun changeFinalNonStaticField() {
    assertProblemFound("Method mock.plugin.field.FieldProblemsContainer.setOnFinalFieldFromNotInitMethod() : void has modifying instruction *putfield* referencing a final field fields.FieldsContainer.finalField : int. This can lead to **IllegalAccessError** exception at runtime.",
        "Attempt to change a final field fields.FieldsContainer.finalField : int"
    )
  }

  @Test
  fun changeFinalStaticField() {
    assertProblemFound("Method mock.plugin.field.FieldProblemsContainer.setOnStaticFinalFieldFromNotClinitMethod() : void has modifying instruction *putstatic* referencing a final field fields.FieldsContainer.staticFinalField : int. This can lead to **IllegalAccessError** exception at runtime.",
        "Attempt to change a final field fields.FieldsContainer.staticFinalField : int"
    )
  }

  @Test
  fun abstractClassInstantiation() {
    assertProblemFound("Method mock.plugin.news.NewProblems.abstractClass() : void has instantiation *new* instruction referencing an abstract class misc.BecomeAbstract. This can lead to **InstantiationError** exception at runtime.",
        "Instantiation of an abstract class misc.BecomeAbstract"
    )
  }

  @Test
  fun interfaceInstantiation() {
    assertProblemFound("Method mock.plugin.news.NewProblems.newInterface() : void has instantiation *new* instruction referencing an interface misc.BecomeInterface. This can lead to **InstantiationError** exception at runtime.",
        "Instantiation of an interface misc.BecomeInterface"
    )

    assertProblemFound("Method mock.plugin.news.NewProblems.newInterface() : void contains an *invokespecial* instruction referencing an unresolved method misc.BecomeInterface.<init>() : void. This can lead to **NoSuchMethodError** exception at runtime.",
        "Invocation of unresolved method misc.BecomeInterface.<init>() : void"
    )
  }

  @Test
  fun nonStaticAccessOfStaticField() {
    assertProblemFound("Method mock.plugin.field.FieldProblemsContainer.instanceAccessOnStatic() : void has non-static access instruction *getfield* referencing a static field fields.FieldsContainer.staticField : int. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Attempt to execute a non-static access instruction *getfield* on a static field fields.FieldsContainer.staticField : int"
    )
  }

  @Test
  fun superClassBecameInterface() {
    assertProblemFound("Class mock.plugin.inheritance.SuperClassBecameInterface has a *super class* misc.BecomeInterface which is actually an *interface*. This can lead to **IncompatibleClassChangeError** at runtime.",
        "Incompatible change of super class misc.BecomeInterface to interface"
    )

    assertProblemFound("Method mock.plugin.inheritance.SuperClassBecameInterface.<init>() : void contains an *invokespecial* instruction referencing an unresolved method misc.BecomeInterface.<init>() : void. This can lead to **NoSuchMethodError** exception at runtime.",
        "Invocation of unresolved method misc.BecomeInterface.<init>() : void"
    )
  }

  @Test
  fun invokeClassMethodOnInterface() {
    assertProblemFound("Method mock.plugin.invokeClassMethodOnInterface.Caller.call(BecomeInterface b) : void has invocation *invokevirtual* instruction referencing a *class* method misc.BecomeInterface.invokeVirtualMethod() : void, but the method's host misc.BecomeInterface is an *interface*. This can lead to **IncompatibleClassChangeError** at runtime.",
        "Incompatible change of class misc.BecomeInterface to interface"
    )
  }

  @Test
  fun superInterfaceBecameClass() {
    assertProblemFound("Interface mock.plugin.inheritance.SuperInterfaceBecomeClass has a *super interface* misc.BecomeClass which is actually a *class*. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Incompatible change of super interface misc.BecomeClass to class"
    )
  }

  @Test
  fun invokeInterfaceMethodOnClass() {
    assertProblemFound("Method mock.plugin.invokeClassMethodOnInterface.Caller.call2(BecomeClass b) : void has invocation *invokeinterface* instruction referencing an *interface* method misc.BecomeClass.invokeInterfaceOnClass() : void, but the method's host misc.BecomeClass is a *class*. This can lead to **IncompatibleClassChangeError** at runtime.",
        "Incompatible change of interface misc.BecomeClass to class"
    )
  }


  @Test
  fun inheritsFromFinalClass() {
    assertProblemFound("Class mock.plugin.finals.InheritFromFinalClass inherits from a final class finals.BecomeFinal. This can lead to **VerifyError** exception at runtime.",
        "Inheritance from a final class finals.BecomeFinal"
    )
  }

  @Test
  fun invokeStaticOnNonStaticMethod() {
    assertProblemFound("Method mock.plugin.invokeStaticOnInstance.InvocationProblemsUser.foo() : void contains an *invokestatic* instruction referencing a non-static method invocation.InvocationProblems.wasStatic() : void. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Attempt to execute an *invokestatic* instruction on a non-static method invocation.InvocationProblems.wasStatic() : void"
    )
  }

  @Test
  fun invokeVirtualOnStaticMethod() {
    assertProblemFound("Method mock.plugin.invokeVirtualOnStatic.SmartEnterProcessorUser.main() : void contains an *invokevirtual* instruction referencing a static method com.intellij.lang.SmartEnterProcessor.commit() : void. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Attempt to execute a non-static instruction *invokevirtual* on a static method com.intellij.lang.SmartEnterProcessor.commit() : void"
    )
  }

  @Test
  fun invokeSpecialOnStaticMethod() {
    assertProblemFound("Method mock.plugin.invokespecial.Child.invokeSpecialOnStaticMethod() : void contains an *invokespecial* instruction referencing a static method invokespecial.AbstractParent.becomeStatic() : void. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Attempt to execute a non-static instruction *invokespecial* on a static method invokespecial.AbstractParent.becomeStatic() : void"
    )
  }

  @Test
  fun invokeInterfaceOnStaticMethod() {
    assertProblemFound("Method mock.plugin.invokeClassMethodOnInterface.Caller.call3(MethodBecameStatic b) : void contains an *invokeinterface* instruction referencing a static method statics.MethodBecameStatic.becomeStatic() : void. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Attempt to execute a non-static instruction *invokeinterface* on a static method statics.MethodBecameStatic.becomeStatic() : void"
    )
  }

  @Test
  fun abstractMethodInvocation() {
    assertProblemFound("Method mock.plugin.invokespecial.Child.bar() : void contains an *invokespecial* instruction referencing a method invokespecial.AbstractParent.foo() : void which doesn't have a non-abstract implementation. This can lead to **AbstractMethodError** exception at runtime.",
        "Attempt to invoke an abstract method invokespecial.AbstractParent.foo() : void"
    )
  }

  @Test
  fun abstractMethodInvocationZeroMaximallySpecificMethods() {
    assertProblemFound("Method mock.plugin.invokespecial.Child.zeroMaximallySpecificMethods() : void contains an *invokespecial* instruction referencing a method invokespecial.SuperInterface.deletedBody() : void which doesn't have a non-abstract implementation. This can lead to **AbstractMethodError** exception at runtime.",
        "Attempt to invoke an abstract method invokespecial.SuperInterface.deletedBody() : void"
    )
  }

  @Test
  fun invokeInterfaceOnPrivateMethod() {
    assertProblemFound("Method mock.plugin.invokeClassMethodOnInterface.Caller.call4(MethodBecameStatic b) : void contains an *invokeinterface* instruction referencing a private method statics.MethodBecameStatic.privateInterfaceMethodTestName() : void. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Attempt to execute an *invokeinterface* instruction on a private method statics.MethodBecameStatic.privateInterfaceMethodTestName() : void"
    )
  }

  @Test
  fun missingDefaultConstructor() {
    assertProblemFound("Method mock.plugin.constructors.MissingDefaultConstructor.<init>() : void contains an *invokespecial* instruction referencing an unresolved method constructors.DeletedDefaultConstructor.<init>() : void. This can lead to **NoSuchMethodError** exception at runtime.",
        "Invocation of unresolved method constructors.DeletedDefaultConstructor.<init>() : void"
    )
  }

  @Test
  fun missingStaticMethod() {
    assertProblemFound("Method mock.plugin.MethodProblems.brokenNonFoundMethod() : void contains an *invokestatic* instruction referencing an unresolved method com.intellij.openapi.actionSystem.AnAction.nonExistingMethod() : void. This can lead to **NoSuchMethodError** exception at runtime.",
        "Invocation of unresolved method com.intellij.openapi.actionSystem.AnAction.nonExistingMethod() : void"
    )
  }

  @Test
  fun missingVirtualMethod() {
    assertProblemFound("Method mock.plugin.non.existing.InvokeRemovedMethod.foo() : void contains an *invokevirtual* instruction referencing an unresolved method non.existing.Child.removedMethod() : void. This can lead to **NoSuchMethodError** exception at runtime. The method might have been declared in the super class belonging to IU-145.500 (non.existing.Parent)",
        "Invocation of unresolved method non.existing.Child.removedMethod() : void"
    )
  }

  @Test
  fun `method signature changed because the generic parameter type of the enclosing class deleted`() {
    assertProblemFound("Method mock.plugin.generics.NoSuchMethodError.error(generics.Base<?> base) : void contains an *invokevirtual* instruction referencing an unresolved method generics.Base.foo(Number) : void. This can lead to **NoSuchMethodError** exception at runtime.",
        "Invocation of unresolved method generics.Base.foo(Number) : void")

    assertProblemFound(
        "Non-abstract class mock.plugin.generics.Subclass inherits from generics.Base<T> but doesn't implement the abstract method foo(T arg0) : void. This can lead to **AbstractMethodError** exception at runtime.",
        "Abstract method generics.Base<T>.foo(T arg0) : void is not implemented"
    )
  }

  @Test
  fun constructorBecamePrivate() {
    assertProblemFound("Method mock.plugin.AccessChangedProblem.foo() : void contains an *invokespecial* instruction referencing a private method com.intellij.openapi.diagnostic.LogUtil.<init>() : void that a class mock.plugin.AccessChangedProblem doesn't have access to. This can lead to **IllegalAccessError** exception at runtime.",
        "Illegal invocation of private method com.intellij.openapi.diagnostic.LogUtil.<init>() : void"
    )
  }

  @Test
  fun illegalAccessToPrivateOrProtectedOrPackagePrivateField() {
    assertProblemFound("Method mock.plugin.field.FieldProblemsContainer.accessPrivateField() : void contains a *getfield* instruction referencing a private field fields.FieldsContainer.privateField : int that a class mock.plugin.field.FieldProblemsContainer doesn't have access to. This can lead to **IllegalAccessError** exception at runtime.",
        "Illegal access to a private field fields.FieldsContainer.privateField : int"
    )

    assertProblemFound("Method mock.plugin.field.FieldProblemsContainer.accessProtectedField() : void contains a *getfield* instruction referencing a protected field fields.otherPackage.OtherFieldsContainer.protectedField : int that a class mock.plugin.field.FieldProblemsContainer doesn't have access to. This can lead to **IllegalAccessError** exception at runtime.",
        "Illegal access to a protected field fields.otherPackage.OtherFieldsContainer.protectedField : int"
    )

    assertProblemFound("Method mock.plugin.field.FieldProblemsContainer.accessPackageField() : void contains a *getfield* instruction referencing a package-private field fields.otherPackage.OtherFieldsContainer.packageField : int that a class mock.plugin.field.FieldProblemsContainer doesn't have access to. This can lead to **IllegalAccessError** exception at runtime.",
        "Illegal access to a package-private field fields.otherPackage.OtherFieldsContainer.packageField : int"
    )
  }

  @Test
  fun nonExistingClassOrInterface() {
    val nonExistingClassLocations = listOf(
        "Method mock.plugin.MethodProblems.brokenReturn() : NonExistingClass",
        "Method mock.plugin.MethodProblems.brokenReturn() : NonExistingClass",
        "Method mock.plugin.MethodProblems.brokenArg(NonExistingClass brokenArg) : void",
        "Method mock.plugin.MethodProblems.brokenLocalVar() : void",
        "Method mock.plugin.MethodProblems.brokenDotClass() : void",
        "Method mock.plugin.MethodProblems.brokenMultiArray() : void",
        "Method mock.plugin.MethodProblems.brokenInvocation() : void",

        "Method mock.plugin.ParentDoesntExist.<init>() : void",
        "Method mock.plugin.arrays.ANewArrayInsn.foo(long l, double d, Object a) : void",
        "Method mock.plugin.field.FieldProblemsContainer.accessUnknownClassOfArray() : void",
        "Method mock.plugin.field.FieldProblemsContainer.accessUnknownClass() : void",
        "Field mock.plugin.FieldTypeNotFound.myNonExistingClass : NonExistingClass",
        "Class mock.plugin.ParentDoesntExist"
    )

    val nonExistingExceptionLocations = listOf(
        "Method mock.plugin.MethodProblems.brokenThrows() : void",
        "Method mock.plugin.MethodProblems.brokenCatch() : void"
    )

    val nonExistingInterfaceLocations = listOf(
        "Class mock.plugin.NotFoundInterface"
    )

    fun rightMissingFullDescription(className: String, location: String) =
        "$location references an unresolved class $className. This can lead to **NoSuchClassError** exception at runtime."

    fun rightMissingShortDescription(className: String) = "Access to unresolved class $className"

    nonExistingClassLocations.forEach {
      assertProblemFound(rightMissingFullDescription("non.existing.NonExistingClass", it), rightMissingShortDescription("non.existing.NonExistingClass"))
    }

    nonExistingExceptionLocations.forEach {
      assertProblemFound(rightMissingFullDescription("non.existing.NonExistingException", it), rightMissingShortDescription("non.existing.NonExistingException"))
    }

    nonExistingInterfaceLocations.forEach {
      assertProblemFound(rightMissingFullDescription("non.existing.NonExistingInterface", it), rightMissingShortDescription("non.existing.NonExistingInterface"))
    }
  }

  @Test
  fun multipleDefaultMethodsOfInvokeSpecial() {
    assertProblemFound("Method mock.plugin.inheritance.SubclassMultipleMethods.baz() : void contains an *invokespecial* instruction referencing a method reference mock.plugin.inheritance.MultipleMethods.foo() : void which has multiple default implementations: inheritance.MultipleDefaultMethod1.foo() : void and inheritance.MultipleDefaultMethod2.foo() : void. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Multiple default implementations of method mock.plugin.inheritance.MultipleMethods.foo() : void"
    )
  }

  @Test
  fun fieldNotFound() {
    assertProblemFound("Method mock.plugin.field.FieldProblemsContainer.accessDeletedField() : void contains a *getfield* instruction referencing an unresolved field fields.FieldsContainer.deletedField : int. This can lead to **NoSuchFieldError** exception at runtime.", "Access to unresolved field fields.FieldsContainer.deletedField : int")
  }

  @Test
  fun illegalAccessToPackagePrivateClass() {
    assertProblemFound("Package-private class access.other.BecamePackagePrivate is not available at mock.plugin.access.IllegalAccess.classBecamePackagePrivate() : void. This can lead to **IllegalAccessError** exception at runtime.",
        "Illegal access to package-private class access.other.BecamePackagePrivate"
    )
  }
}
