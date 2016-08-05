package verifier.tests

import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.IdeManager
import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.domain.PluginManager
import com.intellij.structure.impl.domain.PluginDependencyImpl
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.location.ProblemLocation
import com.jetbrains.pluginverifier.problems.*
import com.jetbrains.pluginverifier.utils.CmdOpts
import com.jetbrains.pluginverifier.utils.VOptionsUtil
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException
import java.util.regex.Pattern

/**
 * NOTE: this test should be run AFTER mock-plugin and mock-idea artifacts
 * are generated.
 * The best way to run this test is a Maven PACKAGE goal (because all
 * the module artifact-dependencies will be resolved automatically)

 * @author Sergey Patrikeev
 */
class VerifierTest {
  private val MY_ACTUAL_PROBLEMS = ImmutableMultimap.builder<Problem, ProblemLocation>()

      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromField("mock/plugin/FieldTypeNotFound", "myNonExistingClass"))
      .put(ClassNotFoundProblem("non/existing/NonExistingInterface"), ProblemLocation.fromClass("mock/plugin/NotFoundInterface"))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenReturn", "()Lnon/existing/NonExistingClass;"))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenArg", "(Lnon/existing/NonExistingClass;)V"))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenLocalVar", "()V"))
      .put(ClassNotFoundProblem("non/existing/NonExistingException"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenThrows", "()V"))
      .put(ClassNotFoundProblem("non/existing/NonExistingException"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenCatch", "()V"))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenDotClass", "()V"))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenMultiArray", "()V"))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenMultiArray", "()V"))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenInvocation", "()V"))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromClass("mock/plugin/ParentDoesntExist"))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/ParentDoesntExist", "<init>", "()V"))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/arrays/ANewArrayInsn", "foo", "(Ljava/lang/Object;)V"))
      .put(ClassNotFoundProblem("non/existing/DeletedClass"), ProblemLocation.fromMethod("mock/plugin/inheritance/PluginClass", "<init>", "()V"))
      .put(ClassNotFoundProblem("non/existing/DeletedClass"), ProblemLocation.fromClass("mock/plugin/inheritance/PluginClass"))
      .put(InheritFromFinalClassProblem("finals/BecomeFinal"), ProblemLocation.fromClass("mock/plugin/finals/InheritFromFinalClass"))
      .put(MethodNotFoundProblem("com/intellij/openapi/actionSystem/AnAction", "nonExistingMethod", "()V"), ProblemLocation.fromMethod("mock/plugin/MethodProblems", "brokenNonFoundMethod", "()V"))
      .put(AbstractClassInstantiationProblem("misc/BecomeAbstract"), ProblemLocation.fromMethod("mock/plugin/news/NewProblems", "abstractClass", "()V"))
      .put(InterfaceInstantiationProblem("misc/BecomeInterface"), ProblemLocation.fromMethod("mock/plugin/news/NewProblems", "newInterface", "()V"))
      .put(MethodNotFoundProblem("misc/BecomeInterface", "<init>", "()V"), ProblemLocation.fromMethod("mock/plugin/news/NewProblems", "newInterface", "()V"))
      .put(MethodNotFoundProblem("invocation/InvocationProblems", "deleted", "()V"), ProblemLocation.fromMethod("mock/plugin/lambda/LambdaProblems", "invokeDeletedFromLambda", "()V"))//lambda problems


      .put(MethodNotImplementedProblem("com/intellij/openapi/components/PersistentStateComponent", "getState", "()Ljava/lang/Object;"), ProblemLocation.fromClass("mock/plugin/NotImplementedProblem"))
      .put(MethodNotImplementedProblem("com/intellij/openapi/components/PersistentStateComponent", "loadState", "(Ljava/lang/Object;)V"), ProblemLocation.fromClass("mock/plugin/NotImplementedProblem"))
      .put(MethodNotImplementedProblem("com/intellij/psi/search/UseScopeEnlarger", "getAdditionalUseScope", "(Lcom/intellij/psi/PsiElement;)Lcom/intellij/psi/search/SearchScope;"), ProblemLocation.fromClass("mock/plugin/abstrackt/NotImplementedAbstractMethod"))
      .put(MethodNotImplementedProblem("com/intellij/openapi/components/PersistentStateComponent", "getState", "()Ljava/lang/Object;"), ProblemLocation.fromClass("mock/plugin/private_and_static/PrivateAndStaticNotImplemented"))
      .put(MethodNotImplementedProblem("com/intellij/openapi/components/PersistentStateComponent", "loadState", "(Ljava/lang/Object;)V"), ProblemLocation.fromClass("mock/plugin/private_and_static/PrivateAndStaticNotImplemented"))
      .put(OverridingFinalMethodProblem("com/intellij/openapi/actionSystem/AnAction", "isEnabledInModalContext", "()Z"), ProblemLocation.fromMethod("mock/plugin/OverrideFinalMethodProblem", "isEnabledInModalContext", "()Z"))
      .put(IllegalMethodAccessProblem("com/intellij/openapi/diagnostic/LogUtil", "<init>", "()V", AccessType.PRIVATE), ProblemLocation.fromMethod("mock/plugin/AccessChangedProblem", "foo", "()V"))
      .put(InvokeVirtualOnStaticMethodProblem("com/intellij/lang/SmartEnterProcessor", "commit", "()V"), ProblemLocation.fromMethod("mock/plugin/invokeVirtualOnStatic/SmartEnterProcessorUser", "main", "()V"))
      .put(InvokeStaticOnInstanceMethodProblem("invocation/InvocationProblems", "wasStatic", "()V"), ProblemLocation.fromMethod("mock/plugin/invokeStaticOnInstance/InvocationProblemsUser", "foo", "()V"))
      .put(FieldNotFoundProblem("fields/FieldsContainer", "deletedField", "I"), ProblemLocation.fromMethod("mock/plugin/field/FieldProblemsContainer", "accessDeletedField", "()V"))//field problems


      .put(IllegalFieldAccessProblem("fields/FieldsContainer", "privateField", "I", AccessType.PRIVATE), ProblemLocation.fromMethod("mock/plugin/field/FieldProblemsContainer", "accessPrivateField", "()V"))
      .put(IllegalFieldAccessProblem("fields/otherPackage/OtherFieldsContainer", "protectedField", "I", AccessType.PROTECTED), ProblemLocation.fromMethod("mock/plugin/field/FieldProblemsContainer", "accessProtectedField", "()V"))
      .put(IllegalFieldAccessProblem("fields/otherPackage/OtherFieldsContainer", "packageField", "I", AccessType.PACKAGE_PRIVATE), ProblemLocation.fromMethod("mock/plugin/field/FieldProblemsContainer", "accessPackageField", "()V"))
      .put(InstanceAccessOfStaticFieldProblem("fields/FieldsContainer", "staticField", "I"), ProblemLocation.fromMethod("mock/plugin/field/FieldProblemsContainer", "instanceAccessOnStatic", "()V"))
      .put(StaticAccessOfInstanceFieldProblem("fields/FieldsContainer", "instanceField", "I"), ProblemLocation.fromMethod("mock/plugin/field/FieldProblemsContainer", "staticAccessOnInstance", "()V"))
      .put(ClassNotFoundProblem("non/existing/NonExistingClass"), ProblemLocation.fromMethod("mock/plugin/field/FieldProblemsContainer", "accessUnknownClass", "()V"))
      .put(ChangeFinalFieldProblem("fields/FieldsContainer", "finalField", "I"), ProblemLocation.fromMethod("mock/plugin/field/FieldProblemsContainer", "setOnFinalFieldFromNotInitMethod", "()V"))
      .put(ChangeFinalFieldProblem("fields/FieldsContainer", "staticFinalField", "I"), ProblemLocation.fromMethod("mock/plugin/field/FieldProblemsContainer", "setOnStaticFinalFieldFromNotClinitMethod", "()V")).build()

  private val RUBY_ACTUAL_PROBLEMS = ImmutableMultimap.builder<Problem, ProblemLocation>()
      .put(MethodNotFoundProblem("com/intellij/util/ui/ReloadableComboBoxPanel", "setDataProvider", "(Lcom/intellij/util/ui/ReloadableComboBoxPanel\$DataProvider;)V"), ProblemLocation.fromMethod("org/jetbrains/plugins/ruby/rails/facet/ui/wizard/ui/tabs/RailsAppSampleConfigurableTab", "createUIComponents", "()V"))
      .put(ClassNotFoundProblem("com/intellij/util/ui/ReloadableComboBoxPanel\$DataProvider"), ProblemLocation.fromClass("org/jetbrains/plugins/ruby/rails/facet/ui/wizard/ui/tabs/RailsAppSampleProvider")).build()

  @Test
  @Throws(Exception::class)
  fun testRuby20160127() {
    val idea = TestData.fetchResource("ideaIU-144.3600.7.zip", true)
    val rubyPlugin = TestData.fetchResource("ruby-8.0.0.20160127.zip", false)
    testFoundProblems(idea, rubyPlugin, RUBY_ACTUAL_PROBLEMS, false, false)
  }

  @Test
  @Throws(Exception::class)
  fun testMyPlugin() {
    val ideaFile = File("build/mocks/after-idea")
    val pluginFile = findLatestFile(File("build/mocks"), "mock-plugin")
    testFoundProblems(ideaFile, pluginFile, MY_ACTUAL_PROBLEMS, true, true)
  }


  @Throws(FileNotFoundException::class)
  private fun findLatestFile(dir: File, fileName: String): File {
    val compile = Pattern.compile("$fileName-(\\d+\\.\\d+).jar")

    val files = dir.listFiles()

    var result: File? = null
    var best = 0.0

    if (files != null) {
      for (f in files) {
        val name = f.name
        val matcher = compile.matcher(name)
        if (matcher.matches()) {
          val group = matcher.group(1)
          val cur = java.lang.Double.parseDouble(group)
          if (best < cur) {
            best = cur
            result = f
          }
        }
      }
    }

    if (result == null) {
      throw FileNotFoundException("Plugin for tests is not found")
    }
    return result
  }

  @Throws(Exception::class)
  private fun testFoundProblems(ideaFile: File, pluginFile: File, actualProblems: ImmutableMultimap<Problem, ProblemLocation>, dummyIdeVersion: Boolean, isMyPlugin: Boolean) {
    val ide: Ide
    if (dummyIdeVersion) {
      ide = IdeManager.getInstance().createIde(ideaFile, DUMMY_IDE_VERSION)
    } else {
      ide = IdeManager.getInstance().createIde(ideaFile)
    }

    val plugin = PluginManager.getInstance().createPlugin(pluginFile)


    var jdkPath: String? = System.getenv("JAVA_HOME")
    if (jdkPath == null) {
      jdkPath = "/usr/lib/jvm/java-8-oracle"
    }

    Resolver.createIdeResolver(ide).use { ideResolver ->
      val pluginDescriptor = PluginDescriptor.ByInstance(plugin)
      val ideDescriptor = IdeDescriptor.ByInstance(ide, ideResolver)
      val vOptions = VOptionsUtil.parseOpts(CmdOpts())
      val results = VManager.verify(VParams(JdkDescriptor.ByFile(jdkPath!!), listOf(Pair<PluginDescriptor, IdeDescriptor>(pluginDescriptor, ideDescriptor)), vOptions, Resolver.getEmptyResolver(), true))
      val result = results.results[0]
      Assert.assertTrue(result is VResult.Problems)

      testFoundProblems((result as VResult.Problems).problems, actualProblems)

      if (isMyPlugin) {
        checkMissingDeps(result)
      }
    }

  }

  private fun checkMissingDeps(result: VResult.Problems) {
    Assert.assertFalse(result.dependenciesGraph.start.missingDependencies.isEmpty())
    Assert.assertTrue(result.dependenciesGraph.start.missingDependencies.containsKey(PluginDependencyImpl("MissingPlugin", true)))
  }

  companion object {

    private val DUMMY_IDE_VERSION = IdeVersion.createIdeVersion("IU-145.500")


    @Throws(Exception::class)
    private fun testFoundProblems(foundProblems: Multimap<Problem, ProblemLocation>, actualProblems: Multimap<Problem, ProblemLocation>) {

      val redundantProblems = HashMultimap.create(foundProblems)

      for ((problem, location) in actualProblems.entries()) {
        Assert.assertTrue("problem $problem should be found at $location, but it isn't", foundProblems.containsKey(problem))
        try {
          val contains = foundProblems.get(problem).contains(location)
          if (!contains) {
            println("ax")
          }
          Assert.assertTrue("problem " + problem + " should be found in the following location " + location + " but it is found in " + foundProblems.get(problem), contains)
        } catch (e: Exception) {
          e.printStackTrace()
        }

        redundantProblems.remove(problem, location)
      }


      if (!redundantProblems.isEmpty) {
        val builder = StringBuilder()
        for ((key, value) in redundantProblems.entries()) {
          //        CodeLocation loc = (CodeLocation) entry.getValue();
          //        System.out.println("xxxxxxx|" + loc.getClassName() + " " + loc.getMethodDescriptor() + " " + loc.getFieldName() + "||||");

          builder.append(key).append(" at ").append(value).append("\n")
        }
        Assert.fail("Found redundant problems which shouldn't be found:\n" + builder.toString())
      }

    }
  }
}
