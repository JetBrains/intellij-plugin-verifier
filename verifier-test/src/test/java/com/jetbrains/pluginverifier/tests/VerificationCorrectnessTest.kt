package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerifier
import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.VerifierExecutor
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.dependencies.resolution.EmptyDependencyFinder
import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.exists
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptorsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsProviderImpl
import com.jetbrains.pluginverifier.plugin.PluginFilesBank
import com.jetbrains.pluginverifier.reporting.verification.Reportage
import com.jetbrains.pluginverifier.reporting.verification.Reporters
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginInfo
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.experimental.ExperimentalApiUsage
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.tests.mocks.TestJdkDescriptorProvider
import com.jetbrains.pluginverifier.verifiers.resolution.DefaultClsResolverProvider
import org.hamcrest.core.Is.`is`
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class VerificationCorrectnessTest {

  companion object {
    var shouldCheckRedundantProblems: Boolean = false

    lateinit var result: VerificationResult.MissingDependencies

    lateinit var actualProblems: Set<CompatibilityProblem>

    lateinit var actualDeprecatedUsages: Set<DeprecatedApiUsage>

    lateinit var actualExperimentalApiUsages: Set<ExperimentalApiUsage>

    lateinit var redundantProblems: MutableSet<CompatibilityProblem>

    lateinit var redundantDeprecated: MutableSet<DeprecatedApiUsage>

    lateinit var redundantExperimentalApiUsages: MutableSet<ExperimentalApiUsage>

    private fun doIdeaAndPluginVerification(ideaFile: Path, pluginFile: Path): VerificationResult {
      val tempDownloadDir = createTempDir().apply { deleteOnExit() }.toPath()
      val pluginFilesBank = PluginFilesBank.create(MarketplaceRepository(URL("http://unused.com")), tempDownloadDir, DiskSpaceSetting(SpaceAmount.ZERO_SPACE))

      val idePlugin = (IdePluginManager.createManager().createPlugin(pluginFile.toFile()) as PluginCreationSuccess).plugin
      val pluginInfo = LocalPluginInfo(idePlugin)
      val jdkPath = TestJdkDescriptorProvider.getJdkPathForTests()
      val tempFolder = Files.createTempDirectory("")
      try {
        val pluginDetailsProvider = PluginDetailsProviderImpl(tempFolder)
        val pluginDetailsCache = PluginDetailsCache(10, pluginFilesBank, pluginDetailsProvider)
        return IdeDescriptor.create(ideaFile, IdeVersion.createIdeVersion("IU-145.500"), null).use { ideDescriptor ->
          val externalClassesPackageFilter = OptionsParser.getExternalClassesPackageFilter(CmdOpts())
          val reportage = object : Reportage {
            override fun createPluginReporters(pluginInfo: PluginInfo, verificationTarget: VerificationTarget) =
                Reporters()

            override fun logVerificationStage(stageMessage: String) = Unit

            override fun logPluginVerificationIgnored(pluginInfo: PluginInfo, verificationTarget: VerificationTarget, reason: String) = Unit

            override fun close() = Unit
          }

          JdkDescriptorsCache().use { jdkDescriptorCache ->
            val tasks = listOf(
                PluginVerifier(
                    pluginInfo,
                    reportage,
                    emptyList(),
                    true,
                    pluginDetailsCache,
                    DefaultClsResolverProvider(
                        EmptyDependencyFinder,
                        jdkDescriptorCache,
                        jdkPath,
                        ideDescriptor,
                        externalClassesPackageFilter
                    ),
                    VerificationTarget.Ide(ideDescriptor.ideVersion),
                    ideDescriptor.brokenPlugins
                )
            )

            VerifierExecutor(4).use { verifierExecutor ->
              verifierExecutor.verify(tasks).single()
            }
          }
        }
      } finally {
        tempFolder.deleteLogged()
      }
    }

    @BeforeClass
    @JvmStatic
    fun verifyMockPlugin() {
      prepareTestEnvironment()
      var ideaFile = Paths.get("build", "mocks", "after-idea")
      if (!ideaFile.exists()) {
        ideaFile = Paths.get("verifier-test", "build", "mocks", "after-idea")
      }
      assertTrue(ideaFile.exists())
      var pluginFile = Paths.get("build", "mocks", "mock-plugin-1.0.jar")
      if (!pluginFile.exists()) {
        pluginFile = Paths.get("verifier-test", "build", "mocks", "mock-plugin-1.0.jar")
      }
      assertTrue(pluginFile.exists())
      val verificationResult = doIdeaAndPluginVerification(ideaFile, pluginFile)
      result = verificationResult as VerificationResult.MissingDependencies
      actualProblems = result.compatibilityProblems
      actualDeprecatedUsages = result.deprecatedUsages
      actualExperimentalApiUsages = result.experimentalApiUsages
      redundantProblems = actualProblems.toMutableSet()
      redundantDeprecated = actualDeprecatedUsages.toMutableSet()
      redundantExperimentalApiUsages = actualExperimentalApiUsages.toMutableSet()
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
        assertTrue("Redundant deprecated usages found: \n$deprecatedMessage", redundantDeprecated.isEmpty())

        val experimentalMessage = redundantExperimentalApiUsages.joinToString(separator = "\n") { "${it.fullDescription}\n" }
        assertTrue("Redundant experimental API found: \n$experimentalMessage", redundantExperimentalApiUsages.isEmpty())
      }
    }
  }

  @Test
  fun checkMissingDeps() {
    val missingDependencies = result.directMissingDependencies
    assertFalse(missingDependencies.isEmpty())
    println(missingDependencies)
    val expectedDep = setOf(MissingDependency(PluginDependencyImpl("MissingPlugin", true, false), "Plugin MissingPlugin is not found"))
    assertEquals(expectedDep, missingDependencies.toSet())
  }

  private fun assertProblemFound(expectedFullDescription: String, expectedShortDescription: String) {
    val problem = actualProblems.find { it.fullDescription == expectedFullDescription }
    if (problem != null && problem.shortDescription != expectedShortDescription) {
      fail("Short description mismatches (expected, actual):\n$expectedShortDescription\n${problem.shortDescription}")
    }
    assertNotNull("Problem with this full description is not found:\n$expectedFullDescription\n\nall problems:\n${actualProblems.joinToString("\n") { it.fullDescription }}", problem)
    redundantProblems.remove(problem)
    assertThat(problem!!.shortDescription, `is`(expectedShortDescription))
    assertThat(problem.fullDescription, `is`(expectedFullDescription))
  }

  private fun assertDeprecatedUsageFound(description: String) {
    val foundDeprecatedUsage = actualDeprecatedUsages.find { description == it.fullDescription }
    assertTrue("Deprecated is not found:\n$description\nall deprecated:\n" + actualDeprecatedUsages.joinToString("\n"), foundDeprecatedUsage != null)
    redundantDeprecated.remove(foundDeprecatedUsage)
  }

  private fun assertExperimentalApiFound(description: String) {
    val foundExperimentalApiUsage = actualExperimentalApiUsages.find { description == it.fullDescription }
    assertTrue("Experimental API usage is not found:\n$description\nall experimental API:\n" + actualExperimentalApiUsages.joinToString("\n"), foundExperimentalApiUsage != null)
    redundantExperimentalApiUsages.remove(foundExperimentalApiUsage)
  }

  @Test
  fun `test is run on the whole class`() {
    shouldCheckRedundantProblems = true
  }

  @Test
  fun `deprecated method is used`() {
    //Auxiliary deprecated usage.
    assertDeprecatedUsageFound("Deprecated class deprecated.DeprecatedClass is referenced in mock.plugin.deprecated.DeprecatedUser.method() : void")
    assertDeprecatedUsageFound("Deprecated method deprecated.DeprecatedMethod.foo(int x) : void is invoked in mock.plugin.deprecated.DeprecatedUser.method() : void")
  }

  @Test
  fun `usage of the method of the deprecated class`() {
    assertDeprecatedUsageFound("Deprecated class deprecated.DeprecatedClass is referenced in mock.plugin.deprecated.DeprecatedUser.method() : void")
  }

  @Test
  fun `deprecated constructor is used`() {
    assertDeprecatedUsageFound("Deprecated constructor deprecated.DeprecatedMethod.<init>() is invoked in mock.plugin.deprecated.DeprecatedUser.method() : void")
  }

  @Test
  fun `overriding deprecated method`() {
    assertDeprecatedUsageFound("Deprecated method deprecated.DeprecatedMethod.foo(int x) : void is overridden in class mock.plugin.deprecated.OverrideDeprecatedMethod")
  }

  @Test
  fun `overriding deprecated interface method`() {
    assertDeprecatedUsageFound("Deprecated interface deprecated.DeprecatedInterface is referenced in mock.plugin.deprecated.OverrideDeprecatedMethod")
    assertDeprecatedUsageFound("Deprecated method deprecated.DeprecatedInterface.bar() : void is overridden in class mock.plugin.deprecated.OverrideDeprecatedMethod")
  }


  @Test
  fun `use default deprecated constructor`() {
    assertDeprecatedUsageFound("Deprecated constructor deprecated.DeprecatedMethod.<init>() is invoked in mock.plugin.deprecated.OverrideDeprecatedMethod.<init>()")
  }

  @Test
  fun `deprecated class is used`() {
    assertDeprecatedUsageFound("Deprecated class deprecated.DeprecatedClass is referenced in mock.plugin.deprecated.DeprecatedUser.clazz() : void")
  }

  @Test
  fun `deprecated with comment class is used`() {
    assertDeprecatedUsageFound("Deprecated class deprecated.DeprecatedWithCommentClass is referenced in mock.plugin.deprecated.DeprecatedUser.clazzWithComment() : void")
  }


  @Test
  fun `deprecated field usage`() {
    assertDeprecatedUsageFound("Deprecated field deprecated.DeprecatedField.x : int is accessed in mock.plugin.deprecated.DeprecatedUser.field() : void")
  }

  @Test
  fun `field of the deprecated class usage`() {
    assertDeprecatedUsageFound("Deprecated class deprecated.DeprecatedClass is referenced in mock.plugin.deprecated.DeprecatedUser.field() : void")
  }

  @Test
  fun notImplementedAbstractMethodFromInterface() {
    assertProblemFound("Concrete class mock.plugin.NotImplementedProblem inherits from com.intellij.openapi.components.PersistentStateComponent but doesn't implement the abstract method getState() : T. This can lead to **AbstractMethodError** exception at runtime.",
        "Abstract method com.intellij.openapi.components.PersistentStateComponent.getState() : T is not implemented"
    )
  }

  @Test
  fun notImplementedPrivateOverridingFromInterface() {
    assertProblemFound("Concrete class mock.plugin.private_and_static.PrivateOverridingNotImplemented inherits from com.intellij.openapi.components.PersistentStateComponent but doesn't implement the abstract method getState() : T. This can lead to **AbstractMethodError** exception at runtime.", "Abstract method com.intellij.openapi.components.PersistentStateComponent.getState() : T is not implemented")
  }

  @Test
  fun notImplementedStaticOverridingFromInterface() {
    assertProblemFound("Concrete class mock.plugin.private_and_static.StaticOverridingNotImplemented inherits from com.intellij.openapi.components.PersistentStateComponent but doesn't implement the abstract method getState() : T. This can lead to **AbstractMethodError** exception at runtime.", "Abstract method com.intellij.openapi.components.PersistentStateComponent.getState() : T is not implemented")
  }


  @Test
  fun notImplementedAbstractMethodFromAbstractClass() {
    assertProblemFound("Concrete class mock.plugin.abstrackt.NotImplementedAbstractMethod inherits from com.intellij.psi.search.UseScopeEnlarger but doesn't implement the abstract method getAdditionalUseScope(PsiElement arg0) : SearchScope. This can lead to **AbstractMethodError** exception at runtime.",
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
    assertProblemFound("Method mock.plugin.field.FieldProblemsContainer.staticAccessOnInstance() : void has static field access instruction *getstatic* referencing an instance field fields.FieldsContainer.instanceField : int, what might have been caused by incompatible change of the field from static to instance. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Attempt to execute static access instruction *getstatic* on instance field fields.FieldsContainer.instanceField : int"
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

    assertProblemFound("Method mock.plugin.news.NewProblems.newInterface() : void contains an *invokespecial* instruction referencing an unresolved constructor misc.BecomeInterface.<init>(). This can lead to **NoSuchMethodError** exception at runtime.",
        "Invocation of unresolved constructor misc.BecomeInterface.<init>()"
    )
  }

  @Test
  fun nonStaticAccessOfStaticField() {
    assertProblemFound("Method mock.plugin.field.FieldProblemsContainer.instanceAccessOnStatic() : void has instance field access instruction *getfield* referencing static field fields.FieldsContainer.staticField : int, what might have been caused by incompatible change of the field to static. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Attempt to execute instance access instruction *getfield* on static field fields.FieldsContainer.staticField : int"
    )
  }

  @Test
  fun superClassBecameInterface() {
    assertProblemFound("Class mock.plugin.inheritance.SuperClassBecameInterface has a *super class* misc.BecomeInterface which is actually an *interface*. This can lead to **IncompatibleClassChangeError** at runtime.",
        "Incompatible change of super class misc.BecomeInterface to interface"
    )

    assertProblemFound("Constructor mock.plugin.inheritance.SuperClassBecameInterface.<init>() contains an *invokespecial* instruction referencing an unresolved constructor misc.BecomeInterface.<init>(). This can lead to **NoSuchMethodError** exception at runtime.",
        "Invocation of unresolved constructor misc.BecomeInterface.<init>()"
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
    assertProblemFound("Method mock.plugin.invokeStaticOnInstance.InvocationProblemsUser.foo() : void contains *invokestatic* instruction referencing instance method invocation.InvocationProblems.wasStatic() : void, what might have been caused by incompatible change of the method from static to instance. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Attempt to execute *invokestatic* instruction on instance method invocation.InvocationProblems.wasStatic() : void"
    )
  }

  @Test
  fun invokeVirtualOnStaticMethod() {
    assertProblemFound("Method mock.plugin.invokeVirtualOnStatic.SmartEnterProcessorUser.main() : void contains an *invokevirtual* instruction referencing a static method com.intellij.lang.SmartEnterProcessor.commit() : void, what might have been caused by incompatible change of the method to static. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Attempt to execute instance instruction *invokevirtual* on a static method com.intellij.lang.SmartEnterProcessor.commit() : void"
    )
  }

  @Test
  fun invokeSpecialOnStaticMethod() {
    assertProblemFound("Method mock.plugin.invokespecial.Child.invokeSpecialOnStaticMethod() : void contains an *invokespecial* instruction referencing a static method invokespecial.AbstractParent.becomeStatic() : void, what might have been caused by incompatible change of the method to static. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Attempt to execute instance instruction *invokespecial* on a static method invokespecial.AbstractParent.becomeStatic() : void"
    )
  }

  @Test
  fun invokeInterfaceOnStaticMethod() {
    assertProblemFound("Method mock.plugin.invokeClassMethodOnInterface.Caller.call3(MethodBecameStatic b) : void contains an *invokeinterface* instruction referencing a static method statics.MethodBecameStatic.becomeStatic() : void, what might have been caused by incompatible change of the method to static. This can lead to **IncompatibleClassChangeError** exception at runtime.",
        "Attempt to execute instance instruction *invokeinterface* on a static method statics.MethodBecameStatic.becomeStatic() : void"
    )
  }

  @Test
  fun abstractMethodInvocation() {
    assertProblemFound("Method mock.plugin.invokespecial.Child.bar() : void contains an *invokespecial* instruction referencing a method invokespecial.AbstractParent.foo() : void which doesn't have an implementation. This can lead to **AbstractMethodError** exception at runtime.",
        "Attempt to invoke an abstract method invokespecial.AbstractParent.foo() : void"
    )
  }

  @Test
  fun abstractMethodInvocationZeroMaximallySpecificMethods() {
    assertProblemFound("Method mock.plugin.invokespecial.Child.zeroMaximallySpecificMethods() : void contains an *invokespecial* instruction referencing a method invokespecial.SuperInterface.deletedBody() : void which doesn't have an implementation. This can lead to **AbstractMethodError** exception at runtime.",
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
    assertProblemFound("Constructor mock.plugin.constructors.MissingDefaultConstructor.<init>() contains an *invokespecial* instruction referencing an unresolved constructor constructors.DeletedDefaultConstructor.<init>(). This can lead to **NoSuchMethodError** exception at runtime.",
        "Invocation of unresolved constructor constructors.DeletedDefaultConstructor.<init>()"
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
    assertProblemFound("Method mock.plugin.non.existing.InvokeRemovedMethod.foo() : void contains an *invokevirtual* instruction referencing an unresolved method invokevirtual.Child.removedMethod() : void. " +
        "This can lead to **NoSuchMethodError** exception at runtime. The method might have been declared in the super class (invokevirtual.Parent)",
        "Invocation of unresolved method invokevirtual.Child.removedMethod() : void"
    )
  }

  @Test
  fun `method signature changed because the generic parameter type of the enclosing class deleted`() {
    assertProblemFound("Method mock.plugin.generics.NoSuchMethodError.error(generics.Base base) : void contains an *invokevirtual* instruction referencing an unresolved method generics.Base.foo(java.lang.Number) : void. This can lead to **NoSuchMethodError** exception at runtime.",
        "Invocation of unresolved method generics.Base.foo(Number) : void")

    assertProblemFound(
        "Concrete class mock.plugin.generics.Subclass inherits from generics.Base but doesn't implement the abstract method foo(T arg0) : void. This can lead to **AbstractMethodError** exception at runtime.",
        "Abstract method generics.Base.foo(T arg0) : void is not implemented"
    )
  }

  @Test
  fun constructorBecamePrivate() {
    assertProblemFound("Method mock.plugin.AccessChangedProblem.foo() : void contains an *invokespecial* instruction referencing a private constructor com.intellij.openapi.diagnostic.LogUtil.<init>() inaccessible to a class mock.plugin.AccessChangedProblem. This can lead to **IllegalAccessError** exception at runtime.",
        "Illegal invocation of private constructor com.intellij.openapi.diagnostic.LogUtil.<init>()"
    )
  }

  @Test
  fun illegalAccessToPrivateOrProtectedOrPackagePrivateField() {
    assertProblemFound("Method mock.plugin.field.FieldProblemsContainer.accessPrivateField() : void contains a *getfield* instruction referencing a private field fields.FieldsContainer.privateField : int inaccessible to a class mock.plugin.field.FieldProblemsContainer. This can lead to **IllegalAccessError** exception at runtime.",
        "Illegal access to a private field fields.FieldsContainer.privateField : int"
    )

    assertProblemFound("Method mock.plugin.field.FieldProblemsContainer.accessProtectedField() : void contains a *getfield* instruction referencing a protected field fields.otherPackage.OtherFieldsContainer.protectedField : int inaccessible to a class mock.plugin.field.FieldProblemsContainer. This can lead to **IllegalAccessError** exception at runtime.",
        "Illegal access to a protected field fields.otherPackage.OtherFieldsContainer.protectedField : int"
    )

    assertProblemFound("Method mock.plugin.field.FieldProblemsContainer.accessPackageField() : void contains a *getfield* instruction referencing a package-private field fields.otherPackage.OtherFieldsContainer.packageField : int inaccessible to a class mock.plugin.field.FieldProblemsContainer. This can lead to **IllegalAccessError** exception at runtime.",
        "Illegal access to a package-private field fields.otherPackage.OtherFieldsContainer.packageField : int"
    )
  }

  @Test
  fun `package removed along with its class`() {
    val expectedFullDesc = """Package 'non' is not found along with its class non.existing.NonExistingClass.
Probably the package 'non' belongs to a library or dependency that is not resolved by the checker.
It is also possible, however, that this package was actually removed from a dependency causing the detected problems. Access to unresolved classes at runtime may lead to **NoSuchClassError**.
The following classes of 'non' are not resolved:
  Class non.existing.NonExistingClass is referenced in
    mock.plugin.FieldTypeNotFound.myNonExistingClass : NonExistingClass
    mock.plugin.MethodProblems.brokenDotClass() : void
    mock.plugin.ParentDoesntExist.<init>()
    mock.plugin.arrays.ANewArrayInsn.foo(long l, double d, Object a) : void
    mock.plugin.field.FieldProblemsContainer.accessUnknownClass() : void
    ...and 7 other places...
"""

    assertProblemFound(expectedFullDesc, "Package 'non' is not found")

    val removedExceptionLocations = listOf(
        "Method mock.plugin.MethodProblems.brokenThrows() : void",
        "Method mock.plugin.MethodProblems.brokenCatch() : void"
    )

    val removedInterfaceLocations = listOf(
        "Interface mock.plugin.NotFoundInterface"
    )

    fun formatFull(className: String, location: String) =
        "$location references an unresolved class $className. This can lead to **NoSuchClassError** exception at runtime."


    removedExceptionLocations.forEach {
      assertProblemFound(formatFull("removedClasses.RemovedException", it), "Access to unresolved class removedClasses.RemovedException")
    }

    removedInterfaceLocations.forEach {
      assertProblemFound(formatFull("removedClasses.RemovedInterface", it), "Access to unresolved class removedClasses.RemovedInterface")
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

  @Test
  fun `method of the IDE class was invoked virtually on plugin's subclass`() {
    assertProblemFound("Method mock.plugin.non.existing.InvokeRemovedMethod.invokeVirtual() : void contains an *invokevirtual* instruction referencing an unresolved method mock.plugin.non.existing.InheritMethod.removedMethod() : void. " +
        "This can lead to **NoSuchMethodError** exception at runtime. The method might have been declared in the super class (invokevirtual.Parent) " +
        "or in the super interfaces (interfaces.SomeInterface, interfaces.SomeInterface2)",
        "Invocation of unresolved method mock.plugin.non.existing.InheritMethod.removedMethod() : void"
    )
  }

  @Test
  fun `static method of the IDE class was invoked on plugin's subclass`() {
    assertProblemFound("Method mock.plugin.non.existing.InvokeRemovedMethod.invokeStatic() : void contains an *invokevirtual* instruction referencing an unresolved method mock.plugin.non.existing.InheritMethod.removedMethod() : void. " +
        "This can lead to **NoSuchMethodError** exception at runtime. The method might have been declared in the super class (invokevirtual.Parent) " +
        "or in the super interfaces (interfaces.SomeInterface, interfaces.SomeInterface2)",
        "Invocation of unresolved method mock.plugin.non.existing.InheritMethod.removedMethod() : void"
    )
  }

  @Test
  fun `instance field of the IDE class was accessed on plugin's subclass`() {
    assertProblemFound("Method mock.plugin.non.existing.AccessRemovedField.foo() : void contains a *getfield* instruction referencing an unresolved field mock.plugin.non.existing.InheritField.removedField : int. " +
        "This can lead to **NoSuchFieldError** exception at runtime. The field might have been declared in the super class (invokevirtual.Parent)",
        "Access to unresolved field mock.plugin.non.existing.InheritField.removedField : int"
    )
  }

  @Test
  fun `final static field of the IDE interface was accessed in plugin`() {
    assertProblemFound("Method mock.plugin.non.existing.AccessRemovedField.foo() : void contains a *getstatic* instruction referencing an unresolved field mock.plugin.non.existing.InheritField.FINAL_FIELD : java.lang.Object. " +
        "This can lead to **NoSuchFieldError** exception at runtime. The field might have been declared in the super class (invokevirtual.Parent) " +
        "or in the super interfaces (interfaces.SomeInterface, interfaces.SomeInterface2)",
        "Access to unresolved field mock.plugin.non.existing.InheritField.FINAL_FIELD : Object"
    )
  }

  @Test
  fun `virtual method became protected in superclass that a class doesn't have access to`() {
    assertProblemFound("Method mock.plugin.access.VirtualAccess.virtualMethodBecameProtected() : void contains an *invokevirtual* instruction referencing access.AccessProblemDerived.foo() : void " +
        "which is resolved to a protected method access.AccessProblemBase.foo() : void inaccessible to a class mock.plugin.access.VirtualAccess. " +
        "This can lead to **IllegalAccessError** exception at runtime.",
        "Illegal invocation of protected method access.AccessProblemBase.foo() : void")
  }

  @Test
  fun `field became protected in superclass that a class doesn't have access to`() {
    assertProblemFound("Method mock.plugin.access.VirtualAccess.inheritedFieldBecameProtected() : void contains a *getfield* instruction referencing access.AccessProblemDerived.x : int " +
        "which is resolved to a protected field access.AccessProblemBase.x : int inaccessible to a class mock.plugin.access.VirtualAccess. " +
        "This can lead to **IllegalAccessError** exception at runtime.",
        "Illegal access to a protected field access.AccessProblemBase.x : int")
  }

  @Test
  fun `kotlin default method without JvmDefault must be re-implemented in Java subclasses`() {
    assertProblemFound("Concrete class mock.plugin.defaults.kotlin.JavaInheritor inherits from defaults.kotlin.I but " +
        "doesn't implement the abstract method noDefault() : int. This can lead to **AbstractMethodError** exception at runtime.",
        "Abstract method defaults.kotlin.I.noDefault() : int is not implemented")
  }

  @Test
  fun `package not resolved`() {
    val expectedFullDescription = """Package 'removedClasses.removedWholePackage' is not found along with its 8 classes.
Probably the package 'removedClasses.removedWholePackage' belongs to a library or dependency that is not resolved by the checker.
It is also possible, however, that this package was actually removed from a dependency causing the detected problems. Access to unresolved classes at runtime may lead to **NoSuchClassError**.
The following classes of 'removedClasses.removedWholePackage' are not resolved (only 5 most used classes are shown, 3 hidden):
  Class removedClasses.removedWholePackage.Removed5 is referenced in
    mock.plugin.removedClasses.User5.<init>()
    mock.plugin.removedClasses.User5.usage1() : void
    mock.plugin.removedClasses.User5.usage4() : void
    mock.plugin.removedClasses.User5.usage5() : void
    mock.plugin.removedClasses.User5.usage8() : void
    ...and 5 other places...
  Class removedClasses.removedWholePackage.Removed1 is referenced in
    mock.plugin.removedClasses.User1.<init>()
    mock.plugin.removedClasses.User1.usage1() : void
    mock.plugin.removedClasses.User1.usage2() : void
    mock.plugin.removedClasses.User1.usage3() : void
    mock.plugin.removedClasses.User1
  Class removedClasses.removedWholePackage.Removed2 is referenced in
    mock.plugin.removedClasses.User2.<init>()
    mock.plugin.removedClasses.User2
  Class removedClasses.removedWholePackage.Removed3 is referenced in
    mock.plugin.removedClasses.User3
    mock.plugin.removedClasses.User3.<init>()
  Class removedClasses.removedWholePackage.Removed4 is referenced in
    mock.plugin.removedClasses.User4
    mock.plugin.removedClasses.User4.<init>()
"""

    val expectedShortDescription = "Package 'removedClasses.removedWholePackage' is not found"
    assertProblemFound(expectedFullDescription, expectedShortDescription)
  }

  @Test
  fun `class removed`() {
    assertProblemFound(
        "Constructor mock.plugin.removedClasses.UsesRemoved.<init>() references an unresolved class removedClasses.RemovedClass. This can lead to **NoSuchClassError** exception at runtime.",
        "Access to unresolved class removedClasses.RemovedClass"
    )
    assertProblemFound(
        "Class mock.plugin.removedClasses.UsesRemoved references an unresolved class removedClasses.RemovedClass. This can lead to **NoSuchClassError** exception at runtime.",
        "Access to unresolved class removedClasses.RemovedClass"
    )
  }

  /**
   * Tests that the checked plugin 'org.some.company.plugin:1.0' is marked as incompatible with checked IU-145.500.
   * See brokenPlugins.txt from 'after-idea'.
   */
  @Test
  fun `checked plugin is marked as incompatible with this IDE`() {
    assertProblemFound(
        "Plugin org.some.company.plugin:1.0 is marked as incompatible with IU-145.500 in the special file 'brokenPlugins.txt' bundled to the IDE distribution. " +
            "This option is used to prevent loading of broken plugins, which may lead to IDE startup errors, if the plugins remain locally installed (in config>/plugins directory) " +
            "and the IDE is updated to newer version where this plugin is no more compatible. The new IDE will refuse to load this plugin with a message " +
            "'The following plugins are incompatible with the current IDE build: org.some.company.plugin' or similar.",
        "Plugin is marked as incompatible with IU-145.500"
    )
  }

  @Test
  fun `scheduled for removal APIs`() {
    assertDeprecatedUsageFound("Deprecated class deprecated.ScheduledForRemovalClass is referenced in mock.plugin.deprecated.ScheduledForRemovalUser.clazz() : void. This class will be removed in 2018.1")

    assertDeprecatedUsageFound("Deprecated constructor deprecated.ScheduledForRemovalMethod.<init>() is invoked in mock.plugin.deprecated.ScheduledForRemovalUser.method() : void. This constructor will be removed in 2018.1")
    assertDeprecatedUsageFound("Deprecated constructor deprecated.ScheduledForRemovalMethod.<init>() is invoked in mock.plugin.deprecated.OverrideScheduledForRemovalMethod.<init>(). This constructor will be removed in 2018.1")

    assertDeprecatedUsageFound("Deprecated field deprecated.ScheduledForRemovalField.x : int is accessed in mock.plugin.deprecated.ScheduledForRemovalUser.field() : void. This field will be removed in 2018.1")
    assertDeprecatedUsageFound("Deprecated class deprecated.ScheduledForRemovalClass is referenced in mock.plugin.deprecated.ScheduledForRemovalUser.field() : void. This class will be removed in 2018.1")

    assertDeprecatedUsageFound("Deprecated method deprecated.ScheduledForRemovalMethod.foo(int x) : void is invoked in mock.plugin.deprecated.ScheduledForRemovalUser.method() : void. This method will be removed in 2018.1")
    assertDeprecatedUsageFound("Deprecated class deprecated.ScheduledForRemovalClass is referenced in mock.plugin.deprecated.ScheduledForRemovalUser.method() : void. This class will be removed in 2018.1")

    assertDeprecatedUsageFound("Deprecated interface deprecated.ScheduledForRemovalInterface is referenced in mock.plugin.deprecated.OverrideScheduledForRemovalMethod. This interface will be removed in 2018.1")

    assertDeprecatedUsageFound("Deprecated method deprecated.ScheduledForRemovalMethod.foo(int x) : void is overridden in class mock.plugin.deprecated.OverrideScheduledForRemovalMethod. This method will be removed in 2018.1")
    assertDeprecatedUsageFound("Deprecated method deprecated.ScheduledForRemovalInterface.bar() : void is overridden in class mock.plugin.deprecated.OverrideScheduledForRemovalMethod. This method will be removed in 2018.1")

    assertDeprecatedUsageFound("Deprecated class deprecated.ScheduledForRemovalClass is referenced in mock.plugin.deprecated.ScheduledForRemovalClassInheritor. This class will be removed in 2018.1")
    assertDeprecatedUsageFound("Deprecated class deprecated.ScheduledForRemovalClass is referenced in mock.plugin.deprecated.ScheduledForRemovalClassInheritor.<init>(). This class will be removed in 2018.1")

    assertDeprecatedUsageFound("Deprecated class deprecated.ScheduledForRemovalClass is referenced in mock.plugin.deprecated.ScheduledForRemovalUser.staticFunOfDeprecatedClass() : void. This class will be removed in 2018.1")
    assertDeprecatedUsageFound("Deprecated class deprecated.ScheduledForRemovalClass is referenced in mock.plugin.deprecated.ScheduledForRemovalUser.staticFieldOfDeprecatedClass() : void. This class will be removed in 2018.1")
  }

  @Test
  fun `experimental APIs`() {
    assertExperimentalApiFound("Experimental API method experimental.ExperimentalApiMethod.foo(int x) : void is invoked in mock.plugin.experimental.ExperimentalApiUser.method(ExperimentalApiMethod) : void. This method can be changed in a future release leading to incompatibilities")
    assertExperimentalApiFound("Experimental API field experimental.ExperimentalApiField.x : int is accessed in mock.plugin.experimental.ExperimentalApiUser.field(ExperimentalApiField) : void. This field can be changed in a future release leading to incompatibilities")
    assertExperimentalApiFound("Experimental API class experimental.ExperimentalApiClass is referenced in mock.plugin.experimental.ExperimentalApiUser.staticFieldOfDeprecatedClass() : void. This class can be changed in a future release leading to incompatibilities")
    assertExperimentalApiFound("Experimental API class experimental.ExperimentalApiClass is referenced in mock.plugin.experimental.ExperimentalApiUser.staticFunOfDeprecatedClass() : void. This class can be changed in a future release leading to incompatibilities")
    assertExperimentalApiFound("Experimental API class experimental.ExperimentalApiClass is referenced in mock.plugin.experimental.ExperimentalApiUser.clazz() : void. This class can be changed in a future release leading to incompatibilities")
  }
}
